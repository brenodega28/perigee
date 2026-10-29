package brenodega28.perigee.core.scoring

import brenodega28.perigee.core.model.AnalysisStep
import brenodega28.perigee.core.model.ArtemisSettings
import brenodega28.perigee.core.model.Category
import brenodega28.perigee.core.model.Codec
import brenodega28.perigee.core.model.Effort
import brenodega28.perigee.core.model.FixExpectation
import brenodega28.perigee.core.model.MeasureUnit
import brenodega28.perigee.core.model.MeasuredValue
import brenodega28.perigee.core.model.Measurements
import brenodega28.perigee.core.model.MetricId
import brenodega28.perigee.core.model.Resolution
import brenodega28.perigee.core.model.ScanContext

class MeasurementsBuilder {
    private val values = mutableMapOf<MetricId, MeasuredValue>()

    fun number(
        metric: MetricId,
        value: Double,
        unit: MeasureUnit,
    ) {
        values[metric] = MeasuredValue.Quantity(value, unit)
    }

    fun flag(
        metric: MetricId,
        value: Boolean,
    ) {
        values[metric] = MeasuredValue.Flag(value)
    }

    fun choice(
        metric: MetricId,
        value: String,
    ) {
        values[metric] = MeasuredValue.Choice(value)
    }

    fun build(): Measurements = Measurements(values.toMap())
}

fun measurements(block: MeasurementsBuilder.() -> Unit): Measurements = MeasurementsBuilder().apply(block).build()

object Fixtures {
    val allSteps = AnalysisStep.entries.toSet()

    val pairedContext = ScanContext(paired = true, completedSteps = allSteps, usedFallback = false)

    val tabletSettings =
        ArtemisSettings(
            resolution = Resolution(2560, 1440),
            fps = 120,
            codec = Codec.Hevc,
            bitDepth = 10,
            hdr = false,
            bitrateMbps = 80.0,
        )

    val tabletMeasurements =
        measurements {
            choice(MetricId.ClientLinkType, "Wifi")
            number(MetricId.ClientWifiBandGhz, 2.4, MeasureUnit.Ghz)
            number(MetricId.ClientWifiWidthMhz, 20.0, MeasureUnit.Mhz)
            number(MetricId.ClientRssiDbm, -58.0, MeasureUnit.Dbm)
            number(MetricId.ClientPhyMbps, 144.0, MeasureUnit.Mbps)
            number(MetricId.ClientRetransmitPercent, 18.0, MeasureUnit.Percent)
            number(MetricId.RttMedianMs, 6.8, MeasureUnit.Ms)
            number(MetricId.JitterP95Ms, 11.2, MeasureUnit.Ms)
            number(MetricId.LossPercent, 0.8, MeasureUnit.Percent)
            number(MetricId.SustainedUdpMbps, 61.0, MeasureUnit.Mbps)
            number(MetricId.BufferbloatMs, 4.0, MeasureUnit.Ms)
            flag(MetricId.WmmEnabled, false)
            flag(MetricId.BandSteeringOn, true)
            number(MetricId.OverlappingBssids, 3.0, MeasureUnit.Count)
            choice(MetricId.HostLinkType, "Ethernet")
            number(MetricId.HostUplinkMbps, 1000.0, MeasureUnit.Mbps)
            flag(MetricId.HostHwEncoderH264, true)
            flag(MetricId.HostHwEncoderHevc, true)
            flag(MetricId.HostHwEncoderAv1, true)
            number(MetricId.EncodeLatencyMs, 3.1, MeasureUnit.Ms)
            flag(MetricId.ApolloTcpPortsOpen, true)
            flag(MetricId.ApolloUdpPortsOpen, true)
            flag(MetricId.HagsEnabled, false)
            choice(MetricId.PowerPlan, "Balanced")
            flag(MetricId.ClientHwDecoderH264, true)
            flag(MetricId.ClientHwDecoderHevc, true)
            flag(MetricId.ClientHwDecoderAv1, true)
            number(MetricId.DecodeLatencyMs, 4.4, MeasureUnit.Ms)
            number(MetricId.RefreshRateHz, 60.0, MeasureUnit.Hz)
            number(MetricId.DisplayMaxRefreshHz, 120.0, MeasureUnit.Hz)
            flag(MetricId.BatterySaverOn, true)
            number(MetricId.ClientDecoderMaxMpixPerSec, 995.0, MeasureUnit.MpixPerSec)
        }

    val moveTo5Ghz =
        FixExpectation(
            id = "net.wifi.move-to-5ghz",
            category = Category.Network,
            effort = Effort.Medium,
            changes =
                measurements {
                    number(MetricId.ClientWifiBandGhz, 5.0, MeasureUnit.Ghz)
                    number(MetricId.ClientWifiWidthMhz, 80.0, MeasureUnit.Mhz)
                    number(MetricId.ClientPhyMbps, 600.0, MeasureUnit.Mbps)
                    number(MetricId.ClientRetransmitPercent, 2.0, MeasureUnit.Percent)
                    number(MetricId.SustainedUdpMbps, 100.0, MeasureUnit.Mbps)
                    number(MetricId.JitterP95Ms, 5.0, MeasureUnit.Ms)
                },
        )

    val lowerBitrate =
        FixExpectation(
            id = "settings.bitrate.lower-to-40",
            category = Category.Settings,
            effort = Effort.Low,
            settingsChange = tabletSettings.copy(bitrateMbps = 40.0),
        )

    val enableWmm =
        FixExpectation(
            id = "net.router.enable-wmm",
            category = Category.Network,
            effort = Effort.Low,
            changes = measurements { flag(MetricId.WmmEnabled, true) },
        )

    val enableHags =
        FixExpectation(
            id = "host.gpu.enable-hags",
            category = Category.Host,
            effort = Effort.Medium,
            changes = measurements { flag(MetricId.HagsEnabled, true) },
        )

    val highPerformancePowerPlan =
        FixExpectation(
            id = "host.power.high-performance",
            category = Category.Host,
            effort = Effort.Low,
            changes = measurements { choice(MetricId.PowerPlan, "HighPerformance") },
        )

    val batterySaverOff =
        FixExpectation(
            id = "client.power.battery-saver-off",
            category = Category.Client,
            effort = Effort.Low,
            changes =
                measurements {
                    flag(MetricId.BatterySaverOn, false)
                    number(MetricId.RefreshRateHz, 120.0, MeasureUnit.Hz)
                },
        )

    val tabletFixes =
        listOf(moveTo5Ghz, lowerBitrate, enableWmm, enableHags, highPerformancePowerPlan, batterySaverOff)

    val wiredSettings = tabletSettings.copy(codec = Codec.Av1)

    val wiredMeasurements =
        measurements {
            choice(MetricId.ClientLinkType, "Ethernet")
            number(MetricId.ClientPhyMbps, 1000.0, MeasureUnit.Mbps)
            number(MetricId.RttMedianMs, 2.0, MeasureUnit.Ms)
            number(MetricId.JitterP95Ms, 1.0, MeasureUnit.Ms)
            number(MetricId.LossPercent, 0.0, MeasureUnit.Percent)
            number(MetricId.SustainedUdpMbps, 400.0, MeasureUnit.Mbps)
            number(MetricId.BufferbloatMs, 1.0, MeasureUnit.Ms)
            flag(MetricId.WmmEnabled, true)
            flag(MetricId.BandSteeringOn, true)
            choice(MetricId.HostLinkType, "Ethernet")
            number(MetricId.HostUplinkMbps, 2500.0, MeasureUnit.Mbps)
            flag(MetricId.HostHwEncoderH264, true)
            flag(MetricId.HostHwEncoderHevc, true)
            flag(MetricId.HostHwEncoderAv1, true)
            number(MetricId.EncodeLatencyMs, 2.0, MeasureUnit.Ms)
            flag(MetricId.ApolloTcpPortsOpen, true)
            flag(MetricId.ApolloUdpPortsOpen, true)
            flag(MetricId.HagsEnabled, true)
            choice(MetricId.PowerPlan, "HighPerformance")
            flag(MetricId.ApolloConfigConsistent, true)
            flag(MetricId.ClientHwDecoderH264, true)
            flag(MetricId.ClientHwDecoderHevc, true)
            flag(MetricId.ClientHwDecoderAv1, true)
            number(MetricId.DecodeLatencyMs, 3.0, MeasureUnit.Ms)
            number(MetricId.RefreshRateHz, 120.0, MeasureUnit.Hz)
            number(MetricId.DisplayMaxRefreshHz, 120.0, MeasureUnit.Hz)
            flag(MetricId.BatterySaverOn, false)
            choice(MetricId.ThermalStatus, "Nominal")
            number(MetricId.ClientDecoderMaxMpixPerSec, 995.0, MeasureUnit.MpixPerSec)
        }
}
