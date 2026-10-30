# Scoring weights — scoring/v1

`weights.json` is the only source of sub-weights, curve endpoints, status thresholds, hard caps, band edges and category maxima. A Gradle task (`generateWeightsSource`) embeds it into generated commonMain source; `ScoringWeights.v1()` parses it with kotlinx.serialization. Editing the JSON regenerates the source.

## How a sub-score becomes points

Each entry maps a measured value (or a derived ratio) to `[0, 1]`: a piecewise-linear curve between `budget` (1.0) and `floor` (0.0), clamped, or a lookup table for booleans and enums. Category points = Σ weight × sub-score, rounded half-up to an Int. Total = sum of the rounded categories, then the lowest matching hard cap lowers it.

Status: sub-score ≥ 0.8 is Pass, < 0.15 is Fail, otherwise Warn. Entries with `maxSeverity: Warn` and all inferred entries never report Fail.

## Sub-weights

| Category | Entry | Weight | Curve / table |
|---|---|---|---|
| Network (35) | client-link-type | 3 | Ethernet 1, Wifi 0.75 |
| | client-wifi-band | 2 | 2.4 GHz → 0, ≥ 5 GHz → 1 |
| | client-wifi-width | 5 | 20 MHz → 0, 80 MHz → 1 |
| | client-rssi | 3 | −85 dBm → 0, −65 dBm → 1 |
| | phy-headroom | 1 | PHY ÷ bitrate: 1.0 → 0, 2.5 → 1 |
| | retransmits | 1 | 15 % → 0, 3 % → 1 |
| | rtt-median | 3 | 30 ms → 0, 5 ms → 1 |
| | jitter-p95 | 2 | 20 ms → 0, 3 ms → 1 |
| | loss-at-target | 8 | 2 % → 0, 0.05 % → 1 |
| | sustained-udp | 2 | sustained ÷ bitrate: 0.6 → 0, 1.3 → 1 |
| | bufferbloat | 1 | 50 ms → 0, 5 ms → 1 |
| | wmm (inferred) | 2 | on 1, off 0 |
| | band-steering (inferred) | 1 | on 1, off 0 |
| | channel-congestion (inferred) | 1 | 10 BSSIDs → 0, 2 → 1 |
| Host (30) | host-link-type | 2 | Ethernet 1, Wifi 0 |
| | host-uplink | 4 | 100 Mbps → 0, 1000 Mbps → 1 |
| | host-encoder (HW HEVC) | 4 | true 1 |
| | host-encoder-av1 | 2 | true 1 |
| | encode-latency | 6 | 16 ms → 0, 3 ms → 1 |
| | apollo-tcp-ports | 3 | open 1 |
| | apollo-udp-ports | 3 | open 1 |
| | hags | 2 | on 1, off 0 |
| | power-plan | 2 | High/Ultimate 1, Balanced 0.5, Power saver 0 |
| | apollo-config | 2 | consistent 1 |
| Client (20) | client-decoder (HW HEVC Main10) | 9 | true 1 |
| | client-decoder-av1 | 4.5 | true 1 |
| | decode-latency | 4 | 20 ms → 0, 4 ms → 1 |
| | client-refresh-rate | 1 | refresh ÷ fps: 0 → 0, 1 → 1 |
| | battery-saver | 0.5 | off 1, on 0 |
| | thermal | 1 | Nominal 1, Light 0.75, Moderate 0.25, Severe 0 |
| Settings (15) | settings-bitrate-fit | 4 | sustained ÷ bitrate: 0.6 → 0, 2.5 → 1 |
| | settings-fps-fit | 4 | display max ÷ fps: 0.5 → 0, 1 → 1 |
| | settings-resolution-fit | 3 | decoder Mpix/s ÷ needed: 0.5 → 0, 1 → 1 |
| | settings-codec | 4 | gap to best shared HW codec (AV1 > HEVC > H.264): 0 → 1, 1 → 0.5, 2+ → 0 |

Curves for RTT, jitter, loss, throughput ratio, encode and decode latency are the SPEC §7.2 values. The other endpoints are builder-chosen.

## Rationale

The weights were tuned so the design's tablet-on-2.4-GHz scenario reproduces exactly: Network 18, Host 25, Client 18, Settings 9 = 70; projected 91; gains +11/+4/+2/+2/+1/+1. Packet loss carries the largest weight because it is the strongest stutter predictor. The Settings bitrate-fit curve is stricter (full marks at 2.5× headroom) than the 1.25× in SPEC §9.2; this lets the 5 GHz and bitrate fixes both move the score without the two fully overlapping. Bitrate and the 5 GHz fix both change the sustained ÷ bitrate ratio, so their gains interact across Network and Settings; the projection re-scores jointly rather than summing.

## Missing metrics

A metric that was not measured scores 0 and its check is `Unknown`; the entry is not excluded or re-normalised, so unmeasured items cost points. Exceptions: Wi-Fi-only entries (band, width, RSSI, retransmits, congestion) get full credit and Pass when the client link is Ethernet. Hard caps never trigger on a missing metric.

## Hard caps

Lowest matching cap wins, and only applies when it lowers the total: no hardware decoder 39, no hardware encoder 39, Apollo ports blocked 39, loss above 2 % 49, host on Wi-Fi and client at or below 2.4 GHz 59.

## Confidence

Low when any of the six steps is not completed. Otherwise Medium when unpaired, on fallback, or when inferred checks earn more than 10 points; else High. Inferred entries total only 4 points in v1, so the 10-point rule cannot trigger with these weights; a test exercises it with a custom weights file.

## Assumed fixture inputs (not shown in the design)

Tablet scenario: bufferbloat 4 ms; 3 overlapping BSSIDs on the channel; H.264 hardware encode and decode available; display max refresh 120 Hz; decoder capacity 995 Mpix/s (4K120); Artemis 2560×1440, 10-bit, SDR; Wi-Fi link type for the client and Ethernet for the host; the 5 GHz fix sets retransmits to 2 % and leaves loss and RTT unchanged. Apollo config consistency and thermal status are left unmeasured, which accounts for 2 Host points and 1 Client point and gives two `Unknown` checks. The battery-saver fix also restores refresh to 120 Hz. Fix efforts: 5 GHz Medium, bitrate Low, WMM Low, HAGS Medium, power plan Low, battery saver Low.

Wired scenario: Ethernet both ends, RTT 2 ms, jitter 1 ms, no loss, 400 Mbps sustained, AV1 chosen, everything enabled; scores 100.
