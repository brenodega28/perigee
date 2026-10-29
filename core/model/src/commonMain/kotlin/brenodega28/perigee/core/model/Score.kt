package brenodega28.perigee.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Status { Pass, Warn, Fail, Unknown }

@Serializable
enum class Category { Network, Host, Client, Settings }

@Serializable
enum class Verdict { Excellent, Playable, Struggling, NotReady }

@Serializable
enum class Confidence { High, Medium, Low }

@Serializable
enum class Effort { Low, Medium, High }

@Serializable
enum class CheckSource { Client, Host, Probe, Inferred }

@Serializable
enum class HardCap {
    NoHardwareDecoder,
    NoHardwareEncoder,
    ApolloPortsBlocked,
    LossAboveLimit,
    HostWifiClient24Ghz,
}

@Serializable
enum class AnalysisStep {
    NetworkLink,
    FindHost,
    LatencyAndJitter,
    ThroughputAtTarget,
    DecoderAndDisplay,
    HostChecks,
}

@Serializable
enum class Codec { H264, Hevc, Av1 }

@Serializable
data class Resolution(
    val width: Int,
    val height: Int,
)

@Serializable
data class ArtemisSettings(
    val resolution: Resolution,
    val fps: Int,
    val codec: Codec,
    val bitDepth: Int,
    val hdr: Boolean,
    val bitrateMbps: Double,
)

@Serializable
data class Check(
    val id: String,
    val value: MeasuredValue?,
    val unit: MeasureUnit?,
    val budget: String,
    val status: Status,
    val source: CheckSource,
    val confidence: Confidence,
)

@Serializable
data class FixExpectation(
    val id: String,
    val category: Category,
    val effort: Effort,
    val changes: Measurements = Measurements(),
    val settingsChange: ArtemisSettings? = null,
)

@Serializable
data class CategoryScore(
    val category: Category,
    val points: Int,
    val max: Int,
)

@Serializable
data class ScanContext(
    val paired: Boolean,
    val completedSteps: Set<AnalysisStep>,
    val usedFallback: Boolean,
)

@Serializable
data class ScanScore(
    val scoringVersion: String,
    val total: Int,
    val categories: List<CategoryScore>,
    val verdict: Verdict,
    val appliedCap: HardCap?,
    val confidence: Confidence,
    val checks: List<Check>,
)

@Serializable
data class FixGain(
    val fixId: String,
    val gain: Int,
)

@Serializable
data class Projection(
    val projectedTotal: Int,
    val gains: List<FixGain>,
)
