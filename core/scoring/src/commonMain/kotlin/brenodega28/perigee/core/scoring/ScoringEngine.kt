package brenodega28.perigee.core.scoring

import brenodega28.perigee.core.model.AnalysisStep
import brenodega28.perigee.core.model.ArtemisSettings
import brenodega28.perigee.core.model.Category
import brenodega28.perigee.core.model.CategoryScore
import brenodega28.perigee.core.model.Check
import brenodega28.perigee.core.model.CheckSource
import brenodega28.perigee.core.model.Codec
import brenodega28.perigee.core.model.Confidence
import brenodega28.perigee.core.model.FixExpectation
import brenodega28.perigee.core.model.FixGain
import brenodega28.perigee.core.model.HardCap
import brenodega28.perigee.core.model.MeasureUnit
import brenodega28.perigee.core.model.MeasuredValue
import brenodega28.perigee.core.model.Measurements
import brenodega28.perigee.core.model.MetricId
import brenodega28.perigee.core.model.Projection
import brenodega28.perigee.core.model.ScanContext
import brenodega28.perigee.core.model.ScanScore
import brenodega28.perigee.core.model.Status
import brenodega28.perigee.core.model.Verdict

class ScoringEngine(
    private val weights: ScoringWeights = ScoringWeights.v1(),
) {
    fun score(
        measurements: Measurements,
        settings: ArtemisSettings,
        context: ScanContext,
    ): ScanScore {
        val evaluations = weights.metrics.map { evaluate(it, measurements, settings) }
        val categories =
            Category.entries.map { category ->
                val earned = evaluations.filter { it.entry.category == category }.sumOf { it.entry.weight * it.score }
                CategoryScore(category, roundPoints(earned), weights.categoryMax.getValue(category))
            }
        val uncapped = categories.sumOf { it.points }
        val cap = lowestCap(uncapped, measurements)
        val total = cap?.maxScore ?: uncapped
        return ScanScore(
            scoringVersion = weights.version,
            total = total,
            categories = categories,
            verdict = verdictFor(total),
            appliedCap = cap?.cap,
            confidence = confidenceFor(context, evaluations),
            checks = evaluations.map { it.toCheck() },
        )
    }

    fun project(
        measurements: Measurements,
        settings: ArtemisSettings,
        context: ScanContext,
        fixes: List<FixExpectation>,
    ): Projection {
        val current = score(measurements, settings, context).total
        val joint = fixes.fold(measurements to settings) { state, fix -> applyFix(state.first, state.second, fix) }
        val projectedTotal = score(joint.first, joint.second, context).total
        val gains =
            fixes
                .mapIndexed { index, fix ->
                    val alone = applyFix(measurements, settings, fix)
                    IndexedGain(index, fix, score(alone.first, alone.second, context).total - current)
                }.sortedWith(compareByDescending<IndexedGain> { it.gain }.thenBy { it.fix.effort }.thenBy { it.index })
                .map { FixGain(it.fix.id, it.gain) }
        return Projection(projectedTotal, gains)
    }

    private class IndexedGain(
        val index: Int,
        val fix: FixExpectation,
        val gain: Int,
    )

    private class Evaluation(
        val entry: MetricWeight,
        val value: MeasuredValue?,
        val score: Double,
        val status: Status,
        val budget: String,
    ) {
        fun toCheck(): Check =
            Check(
                id = entry.id,
                value = value,
                unit = entry.unit,
                budget = budget,
                status = status,
                source = entry.source,
                confidence = if (entry.source == CheckSource.Inferred) Confidence.Medium else Confidence.High,
            )
    }

    private fun applyFix(
        measurements: Measurements,
        settings: ArtemisSettings,
        fix: FixExpectation,
    ): Pair<Measurements, ArtemisSettings> = measurements.overriddenBy(fix.changes) to (fix.settingsChange ?: settings)

    private fun roundPoints(points: Double): Int = (points + ROUNDING_HALF).toInt()

    private fun evaluate(
        entry: MetricWeight,
        measurements: Measurements,
        settings: ArtemisSettings,
    ): Evaluation {
        val value = inputValue(entry, measurements, settings)
        val budget = budgetLabel(entry)
        if (entry.wifiOnly && measurements.choice(MetricId.ClientLinkType) == WIRED_LINK) {
            return Evaluation(entry, value, 1.0, Status.Pass, budget)
        }
        if (value == null) return Evaluation(entry, null, 0.0, Status.Unknown, budget)
        val score = scoreOf(entry, value)
        return Evaluation(entry, value, score, statusFor(entry, score), budget)
    }

    private fun scoreOf(
        entry: MetricWeight,
        value: MeasuredValue,
    ): Double {
        val curve = entry.curve
        if (curve != null) {
            val number = (value as? MeasuredValue.Quantity)?.value ?: return 0.0
            return Curve.of(curve).score(number)
        }
        return entry.table?.get(tableKey(value)) ?: 0.0
    }

    private fun tableKey(value: MeasuredValue): String =
        when (value) {
            is MeasuredValue.Flag -> value.value.toString()
            is MeasuredValue.Choice -> value.value
            is MeasuredValue.Quantity -> value.value.toInt().toString()
        }

    private fun statusFor(
        entry: MetricWeight,
        score: Double,
    ): Status {
        val natural =
            when {
                score >= weights.statusThresholds.passAtOrAbove -> Status.Pass
                score < weights.statusThresholds.failBelow -> Status.Fail
                else -> Status.Warn
            }
        val ceiling = if (entry.inferred) Status.Warn else entry.maxSeverity
        return if (natural == Status.Fail && ceiling != Status.Fail) ceiling else natural
    }

    private fun inputValue(
        entry: MetricWeight,
        measurements: Measurements,
        settings: ArtemisSettings,
    ): MeasuredValue? {
        entry.metric?.let { return measurements[it] }
        val derived = entry.derived ?: return null
        return when (derived) {
            DerivedInput.SustainedOverBitrate -> ratio(measurements.number(MetricId.SustainedUdpMbps), settings.bitrateMbps)
            DerivedInput.PhyOverBitrate -> ratio(measurements.number(MetricId.ClientPhyMbps), settings.bitrateMbps)
            DerivedInput.RefreshOverFps -> ratio(measurements.number(MetricId.RefreshRateHz), settings.fps.toDouble())
            DerivedInput.DisplayMaxOverFps -> ratio(measurements.number(MetricId.DisplayMaxRefreshHz), settings.fps.toDouble())
            DerivedInput.DecoderRateOverNeeded ->
                ratio(measurements.number(MetricId.ClientDecoderMaxMpixPerSec), requiredMpixPerSec(settings))
            DerivedInput.CodecGap -> codecGap(measurements, settings)
        }
    }

    private fun requiredMpixPerSec(settings: ArtemisSettings): Double =
        settings.resolution.width.toDouble() * settings.resolution.height * settings.fps / PIXELS_PER_MPIX

    private fun ratio(
        numerator: Double?,
        denominator: Double,
    ): MeasuredValue? {
        if (numerator == null || denominator <= 0.0) return null
        return MeasuredValue.Quantity(numerator / denominator, MeasureUnit.Ratio)
    }

    private fun codecGap(
        measurements: Measurements,
        settings: ArtemisSettings,
    ): MeasuredValue? {
        val decoders = CODEC_FLAGS.map { (codec, decoder, _) -> codec to measurements.flag(decoder) }
        val encoders = CODEC_FLAGS.map { (codec, _, encoder) -> codec to measurements.flag(encoder) }
        if ((decoders + encoders).all { it.second == null }) return null
        val shared =
            Codec.entries.filter { codec ->
                decoders.first { it.first == codec }.second == true && encoders.first { it.first == codec }.second == true
            }
        val best = shared.maxByOrNull { it.ordinal }
        val gap =
            when {
                best == null || settings.codec !in shared -> MAX_CODEC_GAP
                else -> (best.ordinal - settings.codec.ordinal).coerceAtMost(MAX_CODEC_GAP)
            }
        return MeasuredValue.Quantity(gap.toDouble(), MeasureUnit.Count)
    }

    private fun lowestCap(
        uncapped: Int,
        measurements: Measurements,
    ): CapRule? =
        weights.caps
            .filter { it.maxScore < uncapped && triggered(it, measurements) }
            .minByOrNull { it.maxScore }

    private fun triggered(
        rule: CapRule,
        measurements: Measurements,
    ): Boolean =
        when (rule.cap) {
            HardCap.NoHardwareDecoder -> noneAvailable(measurements, CODEC_FLAGS.map { it.second })
            HardCap.NoHardwareEncoder -> noneAvailable(measurements, CODEC_FLAGS.map { it.third })
            HardCap.ApolloPortsBlocked ->
                measurements.flag(MetricId.ApolloTcpPortsOpen) == false || measurements.flag(MetricId.ApolloUdpPortsOpen) == false
            HardCap.LossAboveLimit -> (measurements.number(MetricId.LossPercent) ?: 0.0) > (rule.limit ?: Double.MAX_VALUE)
            HardCap.HostWifiClient24Ghz ->
                measurements.choice(MetricId.HostLinkType) == WIFI_LINK &&
                    (measurements.number(MetricId.ClientWifiBandGhz) ?: Double.MAX_VALUE) <= (rule.limit ?: 0.0)
        }

    private fun noneAvailable(
        measurements: Measurements,
        metrics: List<MetricId>,
    ): Boolean {
        val flags = metrics.map { measurements.flag(it) }
        return flags.none { it == true } && flags.any { it == false }
    }

    private fun verdictFor(total: Int): Verdict = weights.bands.sortedByDescending { it.minScore }.first { total >= it.minScore }.verdict

    private fun confidenceFor(
        context: ScanContext,
        evaluations: List<Evaluation>,
    ): Confidence {
        if (!context.completedSteps.containsAll(AnalysisStep.entries)) return Confidence.Low
        val inferredPoints = evaluations.filter { it.entry.inferred }.sumOf { it.entry.weight * it.score }
        val fallback = !context.paired || context.usedFallback
        return if (fallback || inferredPoints > weights.confidence.inferredPointsForMedium) Confidence.Medium else Confidence.High
    }

    private fun budgetLabel(entry: MetricWeight): String {
        val curve = entry.curve
        if (curve != null) {
            val direction = if (curve.budget >= curve.floor) "≥" else "≤"
            return "$direction ${plain(curve.budget)}"
        }
        val table = entry.table ?: return ""
        val best = table.values.max()
        return table.filterValues { it == best }.keys.joinToString(" / ")
    }

    private fun plain(number: Double): String = if (number == number.toLong().toDouble()) number.toLong().toString() else number.toString()

    private companion object {
        const val ROUNDING_HALF = 0.5
        const val PIXELS_PER_MPIX = 1_000_000.0
        const val MAX_CODEC_GAP = 2
        const val WIRED_LINK = "Ethernet"
        const val WIFI_LINK = "Wifi"
        val CODEC_FLAGS =
            listOf(
                Triple(Codec.H264, MetricId.ClientHwDecoderH264, MetricId.HostHwEncoderH264),
                Triple(Codec.Hevc, MetricId.ClientHwDecoderHevc, MetricId.HostHwEncoderHevc),
                Triple(Codec.Av1, MetricId.ClientHwDecoderAv1, MetricId.HostHwEncoderAv1),
            )
    }
}
