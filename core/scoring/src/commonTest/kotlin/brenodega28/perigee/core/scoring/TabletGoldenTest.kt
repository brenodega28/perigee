package brenodega28.perigee.core.scoring

import brenodega28.perigee.core.model.Category
import brenodega28.perigee.core.model.Confidence
import brenodega28.perigee.core.model.FixExpectation
import brenodega28.perigee.core.model.MeasureUnit
import brenodega28.perigee.core.model.MetricId
import brenodega28.perigee.core.model.Status
import brenodega28.perigee.core.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TabletGoldenTest {
    private val engine = ScoringEngine()
    private val score = engine.score(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext)

    @Test
    fun scoresCategoriesAndTotal() {
        val points = score.categories.associate { it.category to (it.points to it.max) }
        assertEquals(18 to 35, points.getValue(Category.Network))
        assertEquals(25 to 30, points.getValue(Category.Host))
        assertEquals(18 to 20, points.getValue(Category.Client))
        assertEquals(9 to 15, points.getValue(Category.Settings))
        assertEquals(70, score.total)
        assertEquals(Verdict.Playable, score.verdict)
        assertNull(score.appliedCap)
        assertEquals(Confidence.High, score.confidence)
        assertEquals("scoring/v1", score.scoringVersion)
    }

    @Test
    fun checkStatusesMatchDesign() {
        val statuses = score.checks.associate { it.id to it.status }
        val expected =
            mapOf(
                "host-uplink" to Status.Pass,
                "apollo-tcp-ports" to Status.Pass,
                "apollo-udp-ports" to Status.Pass,
                "host-encoder" to Status.Pass,
                "encode-latency" to Status.Pass,
                "client-decoder" to Status.Pass,
                "decode-latency" to Status.Pass,
                "client-wifi-band" to Status.Fail,
                "hags" to Status.Warn,
                "power-plan" to Status.Warn,
                "client-refresh-rate" to Status.Warn,
                "rtt-median" to Status.Pass,
                "jitter-p95" to Status.Warn,
                "loss-at-target" to Status.Warn,
                "sustained-udp" to Status.Warn,
            )
        expected.forEach { (id, status) -> assertEquals(status, statuses[id], id) }
    }

    @Test
    fun inferredChecksNeverFail() {
        val inferred = score.checks.filter { it.source == brenodega28.perigee.core.model.CheckSource.Inferred }
        assertEquals(setOf("wmm", "band-steering", "channel-congestion"), inferred.map { it.id }.toSet())
        assertEquals(Status.Warn, inferred.first { it.id == "wmm" }.status)
    }

    @Test
    fun unmeasuredChecksAreUnknown() {
        val statuses = score.checks.associate { it.id to it.status }
        assertEquals(Status.Unknown, statuses["apollo-config"])
        assertEquals(Status.Unknown, statuses["thermal"])
    }

    @Test
    fun projectsToNinetyOneWithDesignGainOrder() {
        val projection = engine.project(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext, Fixtures.tabletFixes)
        assertEquals(91, projection.projectedTotal)
        assertEquals(
            listOf(
                Fixtures.moveTo5Ghz.id to 11,
                Fixtures.lowerBitrate.id to 4,
                Fixtures.enableWmm.id to 2,
                Fixtures.enableHags.id to 2,
                Fixtures.highPerformancePowerPlan.id to 1,
                Fixtures.batterySaverOff.id to 1,
            ),
            projection.gains.map { it.fixId to it.gain },
        )
    }

    @Test
    fun bitrateFixAffectsNetworkAndJointProjectionEqualsSumOfGains() {
        val bitrateAlone = engine.score(Fixtures.tabletMeasurements, Fixtures.lowerBitrate.settingsChange!!, Fixtures.pairedContext)
        val networkAlone = bitrateAlone.categories.first { it.category == Category.Network }.points
        assertEquals(true, networkAlone > 18)

        val pair = listOf(Fixtures.moveTo5Ghz, Fixtures.lowerBitrate)
        val projection = engine.project(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext, pair)
        assertEquals(70 + projection.gains.sumOf { it.gain }, projection.projectedTotal)
        assertEquals(85, projection.projectedTotal)

        val all = engine.project(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext, Fixtures.tabletFixes)
        assertEquals(70 + all.gains.sumOf { it.gain }, all.projectedTotal)
    }

    @Test
    fun equalGainsBreakByEffortThenInputOrder() {
        val reordered =
            listOf(
                Fixtures.batterySaverOff,
                Fixtures.enableHags,
                Fixtures.highPerformancePowerPlan,
                Fixtures.enableWmm,
            )
        val projection = engine.project(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext, reordered)
        assertEquals(
            listOf(Fixtures.enableWmm.id, Fixtures.enableHags.id, Fixtures.batterySaverOff.id, Fixtures.highPerformancePowerPlan.id),
            projection.gains.map { it.fixId },
        )
    }

    @Test
    fun moveTo5GhzExpectsDesignValues() {
        val changes = Fixtures.moveTo5Ghz.changes
        assertEquals(5.0, changes.number(MetricId.ClientWifiBandGhz))
        assertEquals(80.0, changes.number(MetricId.ClientWifiWidthMhz))
        assertEquals(true, (changes.number(MetricId.ClientPhyMbps) ?: 0.0) >= 600.0)
        assertEquals(true, (changes.number(MetricId.ClientRetransmitPercent) ?: 100.0) < 3.0)
        assertEquals(true, (changes.number(MetricId.SustainedUdpMbps) ?: 0.0) >= 100.0)
        assertEquals(true, (changes.number(MetricId.JitterP95Ms) ?: 100.0) <= 5.0)
        assertEquals(MeasureUnit.Ghz, (changes[MetricId.ClientWifiBandGhz] as brenodega28.perigee.core.model.MeasuredValue.Quantity).unit)
    }

    @Test
    fun noFixesProjectsCurrentScore() {
        val projection = engine.project(Fixtures.tabletMeasurements, Fixtures.tabletSettings, Fixtures.pairedContext, emptyList<FixExpectation>())
        assertEquals(70, projection.projectedTotal)
        assertEquals(emptyList(), projection.gains)
    }
}
