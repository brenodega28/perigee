package brenodega28.perigee.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MetricId {
    ClientLinkType,
    ClientWifiBandGhz,
    ClientWifiWidthMhz,
    ClientRssiDbm,
    ClientPhyMbps,
    ClientRetransmitPercent,
    RttMedianMs,
    JitterP95Ms,
    LossPercent,
    SustainedUdpMbps,
    BufferbloatMs,
    WmmEnabled,
    BandSteeringOn,
    OverlappingBssids,
    HostLinkType,
    HostUplinkMbps,
    HostHwEncoderH264,
    HostHwEncoderHevc,
    HostHwEncoderAv1,
    EncodeLatencyMs,
    ApolloTcpPortsOpen,
    ApolloUdpPortsOpen,
    HagsEnabled,
    PowerPlan,
    ApolloConfigConsistent,
    ClientHwDecoderH264,
    ClientHwDecoderHevc,
    ClientHwDecoderAv1,
    DecodeLatencyMs,
    RefreshRateHz,
    DisplayMaxRefreshHz,
    BatterySaverOn,
    ThermalStatus,
    ClientDecoderMaxMpixPerSec,
}

@Serializable
enum class MeasureUnit {
    Ms,
    Mbps,
    Percent,
    Ghz,
    Mhz,
    Dbm,
    Hz,
    Count,
    MpixPerSec,
    Ratio,
}

@Serializable
sealed interface MeasuredValue {
    @Serializable
    data class Quantity(
        val value: Double,
        val unit: MeasureUnit,
    ) : MeasuredValue

    @Serializable
    data class Flag(
        val value: Boolean,
    ) : MeasuredValue

    @Serializable
    data class Choice(
        val value: String,
    ) : MeasuredValue
}

@Serializable
data class Measurements(
    val values: Map<MetricId, MeasuredValue> = emptyMap(),
) {
    operator fun get(metric: MetricId): MeasuredValue? = values[metric]

    fun number(metric: MetricId): Double? = (values[metric] as? MeasuredValue.Quantity)?.value

    fun flag(metric: MetricId): Boolean? = (values[metric] as? MeasuredValue.Flag)?.value

    fun choice(metric: MetricId): String? = (values[metric] as? MeasuredValue.Choice)?.value

    fun overriddenBy(changes: Measurements): Measurements = Measurements(values + changes.values)

    fun without(metric: MetricId): Measurements = Measurements(values - metric)
}
