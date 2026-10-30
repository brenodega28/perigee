package brenodega28.perigee.core.scoring

import brenodega28.perigee.core.model.AnalysisStep
import brenodega28.perigee.core.model.Category
import brenodega28.perigee.core.model.Confidence
import brenodega28.perigee.core.model.HardCap
import brenodega28.perigee.core.model.MeasureUnit
import brenodega28.perigee.core.model.Measurements
import brenodega28.perigee.core.model.MetricId
import brenodega28.perigee.core.model.Status
import brenodega28.perigee.core.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EngineBehaviorTest {
    private val engine = ScoringEngine()

    private fun scoreOf(
        measurements: Measurements,
        context: brenodega28.perigee.core.model.ScanContext = Fixtures.pairedContext,
    ) = engine.score(measurements, Fixtures.wiredSettings, context)

    private fun ScoringEngine.scoreWired(measurements: Measurements) =
        score(measurements, Fixtures.wiredSettings, Fixtures.pairedContext)

    private fun wiredWith(build: MeasurementsBuilder.() -> Unit) =
        Fixtures.wiredMeasurements.overriddenBy(measurements(build))

    private fun rttOnlyEngine(rtt: Double): Int {
        val weights =
            ScoringWeights.parse(
                """
                {
                  "version": "test",
                  "categoryMax": { "Network": 100, "Host": 0, "Client": 0, "Settings": 0 },
                  "statusThresholds": { "passAtOrAbove": 0.8, "failBelow": 0.15 },
                  "confidence": { "inferredPointsForMedium": 10.0 },
                  "bands": [
                    { "verdict": "Excellent", "minScore": 80 },
                    { "verdict": "Playable", "minScore": 60 },
                    { "verdict": "Struggling", "minScore": 40 },
                    { "verdict": "NotReady", "minScore": 0 }
                  ],
                  "caps": [],
                  "metrics": [
                    {
                      "id": "rtt",
                      "category": "Network",
                      "weight": 100.0,
                      "metric": "RttMedianMs",
                      "curve": { "budget": 0.0, "floor": 100.0 },
                      "source": "Probe"
                    }
                  ]
                }
                """.trimIndent(),
            )
        val rttOnly = measurements { number(MetricId.RttMedianMs, rtt, MeasureUnit.Ms) }
        val total = ScoringEngine(weights).scoreWired(rttOnly)
        return total.total * 1000 + total.verdict.ordinal
    }

    @Test
    fun wiredAllPassScoresExcellentWithNothingToFix() {
        val score = scoreOf(Fixtures.wiredMeasurements)
        assertEquals(100, score.total)
        assertEquals(Verdict.Excellent, score.verdict)
        assertEquals(true, score.checks.all { it.status == Status.Pass })
        val projection =
            engine.project(Fixtures.wiredMeasurements, Fixtures.wiredSettings, Fixtures.pairedContext, emptyList())
        assertEquals(100, projection.projectedTotal)
        assertEquals(emptyList(), projection.gains)
    }

    @Test
    fun bandEdges() {
        val expected =
            mapOf(
                61.0 to (39 to Verdict.NotReady),
                60.0 to (40 to Verdict.Struggling),
                41.0 to (59 to Verdict.Struggling),
                40.0 to (60 to Verdict.Playable),
                21.0 to (79 to Verdict.Playable),
                20.0 to (80 to Verdict.Excellent),
            )
        expected.forEach { (rtt, result) ->
            val encoded = rttOnlyEngine(rtt)
            assertEquals(result.first, encoded / 1000, "total at rtt=$rtt")
            assertEquals(result.second, Verdict.entries[encoded % 1000], "verdict at rtt=$rtt")
        }
    }

    @Test
    fun curvesClampOnBothEnds() {
        val descending = Curve(budget = 5.0, floor = 30.0)
        assertEquals(1.0, descending.score(0.0))
        assertEquals(1.0, descending.score(5.0))
        assertEquals(0.5, descending.score(17.5))
        assertEquals(0.0, descending.score(30.0))
        assertEquals(0.0, descending.score(500.0))
        val ascending = Curve(budget = 1.3, floor = 0.6)
        assertEquals(1.0, ascending.score(9.0))
        assertEquals(0.0, ascending.score(0.1))
    }

    @Test
    fun everyHardCapAppliesItsLimit() {
        val cases =
            listOf(
                HardCap.NoHardwareDecoder to 39,
                HardCap.NoHardwareEncoder to 39,
                HardCap.ApolloPortsBlocked to 39,
                HardCap.LossAboveLimit to 49,
                HardCap.HostWifiClient24Ghz to 59,
            )
        val triggers =
            mapOf(
                HardCap.NoHardwareDecoder to
                    Fixtures.wiredMeasurements.overriddenBy(
                        measurements {
                            flag(MetricId.ClientHwDecoderH264, false)
                            flag(MetricId.ClientHwDecoderHevc, false)
                            flag(MetricId.ClientHwDecoderAv1, false)
                        },
                    ),
                HardCap.NoHardwareEncoder to
                    Fixtures.wiredMeasurements.overriddenBy(
                        measurements {
                            flag(MetricId.HostHwEncoderH264, false)
                            flag(MetricId.HostHwEncoderHevc, false)
                            flag(MetricId.HostHwEncoderAv1, false)
                        },
                    ),
                HardCap.ApolloPortsBlocked to wiredWith { flag(MetricId.ApolloUdpPortsOpen, false) },
                HardCap.LossAboveLimit to wiredWith { number(MetricId.LossPercent, 2.5, MeasureUnit.Percent) },
                HardCap.HostWifiClient24Ghz to
                    Fixtures.wiredMeasurements.overriddenBy(
                        measurements {
                            choice(MetricId.ClientLinkType, "Wifi")
                            choice(MetricId.HostLinkType, "Wifi")
                            number(MetricId.ClientWifiBandGhz, 2.4, MeasureUnit.Ghz)
                            number(MetricId.ClientWifiWidthMhz, 80.0, MeasureUnit.Mhz)
                            number(MetricId.ClientRssiDbm, -50.0, MeasureUnit.Dbm)
                            number(MetricId.ClientRetransmitPercent, 1.0, MeasureUnit.Percent)
                            number(MetricId.OverlappingBssids, 0.0, MeasureUnit.Count)
                        },
                    ),
            )
        cases.forEach { (cap, limit) ->
            val score = scoreOf(triggers.getValue(cap))
            assertEquals(cap, score.appliedCap, "cap $cap")
            assertEquals(limit, score.total, "total with $cap")
        }
    }

    @Test
    fun lowestCapWinsWhenSeveralApply() {
        val broken =
            Fixtures.wiredMeasurements.overriddenBy(
                measurements {
                    number(MetricId.LossPercent, 3.0, MeasureUnit.Percent)
                    flag(MetricId.ApolloTcpPortsOpen, false)
                    choice(MetricId.HostLinkType, "Wifi")
                    choice(MetricId.ClientLinkType, "Wifi")
                    number(MetricId.ClientWifiBandGhz, 2.4, MeasureUnit.Ghz)
                },
            )
        val score = scoreOf(broken)
        assertEquals(39, score.total)
        assertEquals(HardCap.ApolloPortsBlocked, score.appliedCap)
        val lossAndWifi = broken.overriddenBy(measurements { flag(MetricId.ApolloTcpPortsOpen, true) })
        val second = scoreOf(lossAndWifi)
        assertEquals(49, second.total)
        assertEquals(HardCap.LossAboveLimit, second.appliedCap)
    }

    @Test
    fun confidenceLevels() {
        assertEquals(Confidence.High, scoreOf(Fixtures.wiredMeasurements).confidence)
        val fallback = Fixtures.pairedContext.copy(usedFallback = true)
        assertEquals(Confidence.Medium, scoreOf(Fixtures.wiredMeasurements, fallback).confidence)
        val unpaired = Fixtures.pairedContext.copy(paired = false)
        assertEquals(Confidence.Medium, scoreOf(Fixtures.wiredMeasurements, unpaired).confidence)
        val stepsWithoutHost = AnalysisStep.entries.toSet() - AnalysisStep.HostChecks
        val skipped = Fixtures.pairedContext.copy(completedSteps = stepsWithoutHost)
        assertEquals(Confidence.Low, scoreOf(Fixtures.wiredMeasurements, skipped).confidence)
    }

    @Test
    fun inferredChecksContributingOverThresholdLowerConfidenceToMedium() {
        val text = ScoringWeightsText.inferredHeavy
        val heavy = ScoringEngine(ScoringWeights.parse(text))
        val score = heavy.scoreWired(measurements { flag(MetricId.WmmEnabled, true) })
        assertEquals(Confidence.Medium, score.confidence)
        val light = heavy.scoreWired(measurements { flag(MetricId.WmmEnabled, false) })
        assertEquals(Confidence.High, light.confidence)
    }

    @Test
    fun missingMetricScoresZeroAndIsUnknown() {
        val full = scoreOf(Fixtures.wiredMeasurements)
        val missing = scoreOf(Fixtures.wiredMeasurements.without(MetricId.RttMedianMs))
        val rtt = missing.checks.first { it.id == "rtt-median" }
        assertEquals(Status.Unknown, rtt.status)
        assertNull(rtt.value)
        assertEquals(97, missing.total)
        assertEquals(Category.Network, missing.categories.first { it.points < it.max }.category)
        assertEquals(100, full.total)
    }

    @Test
    fun emptyMeasurementsDoNotCrash() {
        val score = engine.score(Measurements(), Fixtures.tabletSettings, Fixtures.pairedContext)
        assertEquals(0, score.total)
        assertNull(score.appliedCap)
        assertEquals(true, score.checks.all { it.status == Status.Unknown })
    }

    @Test
    fun v1WeightsMatchCategoryMaxima() {
        val weights = ScoringWeights.v1()
        assertEquals("scoring/v1", weights.version)
        Category.entries.forEach { category ->
            val sum = weights.metrics.filter { it.category == category }.sumOf { it.weight }
            assertEquals(weights.categoryMax.getValue(category).toDouble(), sum, "sub-weights of $category")
        }
    }
}

private object ScoringWeightsText {
    val inferredHeavy =
        """
        {
          "version": "test",
          "categoryMax": { "Network": 30, "Host": 0, "Client": 0, "Settings": 0 },
          "statusThresholds": { "passAtOrAbove": 0.8, "failBelow": 0.15 },
          "confidence": { "inferredPointsForMedium": 10.0 },
          "bands": [ { "verdict": "NotReady", "minScore": 0 } ],
          "caps": [],
          "metrics": [
            {
              "id": "wmm",
              "category": "Network",
              "weight": 30.0,
              "metric": "WmmEnabled",
              "table": { "true": 1.0, "false": 0.0 },
              "inferred": true,
              "source": "Inferred"
            }
          ]
        }
        """.trimIndent()
}
