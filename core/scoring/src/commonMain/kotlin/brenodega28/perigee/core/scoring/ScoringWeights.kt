package brenodega28.perigee.core.scoring

import brenodega28.perigee.core.model.Category
import brenodega28.perigee.core.model.CheckSource
import brenodega28.perigee.core.model.HardCap
import brenodega28.perigee.core.model.MeasureUnit
import brenodega28.perigee.core.model.MetricId
import brenodega28.perigee.core.model.Status
import brenodega28.perigee.core.model.Verdict
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class DerivedInput {
    SustainedOverBitrate,
    PhyOverBitrate,
    RefreshOverFps,
    DisplayMaxOverFps,
    DecoderRateOverNeeded,
    CodecGap,
}

@Serializable
data class CurveSpec(
    val budget: Double,
    val floor: Double,
)

@Serializable
data class StatusThresholds(
    val passAtOrAbove: Double,
    val failBelow: Double,
)

@Serializable
data class ConfidenceRules(
    val inferredPointsForMedium: Double,
)

@Serializable
data class BandEdge(
    val verdict: Verdict,
    val minScore: Int,
)

@Serializable
data class CapRule(
    val cap: HardCap,
    val maxScore: Int,
    val limit: Double? = null,
)

@Serializable
data class MetricWeight(
    val id: String,
    val category: Category,
    val weight: Double,
    val metric: MetricId? = null,
    val derived: DerivedInput? = null,
    val curve: CurveSpec? = null,
    val table: Map<String, Double>? = null,
    val unit: MeasureUnit? = null,
    val wifiOnly: Boolean = false,
    val inferred: Boolean = false,
    val maxSeverity: Status = Status.Fail,
    val source: CheckSource,
)

@Serializable
data class ScoringWeights(
    val version: String,
    val categoryMax: Map<Category, Int>,
    val statusThresholds: StatusThresholds,
    val confidence: ConfidenceRules,
    val bands: List<BandEdge>,
    val caps: List<CapRule>,
    val metrics: List<MetricWeight>,
) {
    companion object {
        fun parse(text: String): ScoringWeights = Json.decodeFromString(serializer(), text)

        fun v1(): ScoringWeights = parse(EmbeddedWeights.JSON)
    }
}
