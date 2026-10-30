package brenodega28.perigee.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelSerializationTest {
    @Test
    fun measurementsRoundTrip() {
        val original =
            Measurements(
                mapOf(
                    MetricId.RttMedianMs to MeasuredValue.Quantity(6.8, MeasureUnit.Ms),
                    MetricId.HagsEnabled to MeasuredValue.Flag(false),
                    MetricId.PowerPlan to MeasuredValue.Choice("Balanced"),
                ),
            )
        val decoded = Json.decodeFromString(Measurements.serializer(), Json.encodeToString(Measurements.serializer(), original))
        assertEquals(original, decoded)
    }

    @Test
    fun scanScoreRoundTrip() {
        val original =
            ScanScore(
                scoringVersion = "scoring/v1",
                total = 70,
                categories = listOf(CategoryScore(Category.Network, 18, 35)),
                verdict = Verdict.Playable,
                appliedCap = null,
                confidence = Confidence.High,
                checks =
                    listOf(
                        Check("rtt-median", MeasuredValue.Quantity(6.8, MeasureUnit.Ms), MeasureUnit.Ms, "≥ 5", Status.Pass, CheckSource.Probe, Confidence.High),
                    ),
            )
        val decoded = Json.decodeFromString(ScanScore.serializer(), Json.encodeToString(ScanScore.serializer(), original))
        assertEquals(original, decoded)
    }

    @Test
    fun overriddenByReplacesOnlyGivenMetrics() {
        val base = Measurements(mapOf(MetricId.HagsEnabled to MeasuredValue.Flag(false), MetricId.WmmEnabled to MeasuredValue.Flag(false)))
        val changed = base.overriddenBy(Measurements(mapOf(MetricId.HagsEnabled to MeasuredValue.Flag(true))))
        assertEquals(true, changed.flag(MetricId.HagsEnabled))
        assertEquals(false, changed.flag(MetricId.WmmEnabled))
    }
}
