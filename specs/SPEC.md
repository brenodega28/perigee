# Perigee — Stream Readiness Analyzer

**Specification · v0.1 (draft)** · 2026-10-28
Status: proposed · Owner: Breno · License: open source (see §15)

---

## 1. Mission

> **Tell anyone who streams games with Apollo and Artemis, in under two minutes, how ready their setup is — as one honest number from 0 to 100 — and exactly what to change to make it better.**

Self-hosted game streaming fails in ways that are hard to see: a tablet quietly parked on 2.4 GHz, a bitrate set above what the link can carry, a host on a Balanced power plan, a TV with battery-saver-style refresh caps. Today players diagnose this by trial, forum threads and guesswork. Perigee replaces that with measurement.

### Principles

1. **Measure, don't guess.** Every point in the score traces back to a measured value with a stated budget. No hidden heuristics.
2. **Both ends of the stream.** The host and the client see different halves of the problem. Perigee runs on both and pairs them, so the score reflects the full path.
3. **Fixes ranked by impact.** Each recommendation shows how many points it is worth, computed by re-scoring with the fixed value — not a fixed guess.
4. **Local and private.** Everything runs on the user's LAN. No account, no cloud, no telemetry by default.
5. **Never change a setting silently.** Perigee may apply host-side fixes, but only after explicit confirmation, and every change is reversible.

### Success looks like

- A player gets a score, a recommended Artemis profile and a ranked fix list in **≤ 90 s** from opening the app.
- After applying the top fixes, the **projected score is within ±5 points** of the re-tested score in ≥ 80 % of cases.
- Users report the recommended profile streams without visible stutter in ≥ 85 % of feedback responses.

---

## 2. Scope

### 2.1 Goals (v1.0)

| # | Goal |
|---|------|
| G1 | Windows desktop app that runs on the **Apollo host** and analyzes host + network |
| G2 | Android app (phone/tablet) that runs on the **Artemis client** and analyzes client + network |
| G3 | Android TV variant with full D-pad navigation (reference device: NVIDIA Shield) |
| G4 | LAN pairing between desktop and Android so each side shows the full end-to-end picture |
| G5 | 0–100 score with four-category breakdown, verdict band and projected score |
| G6 | Recommended Artemis profile (resolution · fps · codec · bitrate) for the measured environment |
| G7 | Ranked enhancements with guides; one-click apply for safe host-side fixes |
| G8 | Scan history with before/after comparison |

### 2.2 Non-goals (v1.0)

- **No streaming.** Perigee does not stream video; it measures the conditions for it.
- **No internet / WAN streaming analysis.** v1 assumes host and client on the same LAN. (Planned later, §Roadmap Phase 8.)
- **No macOS or Linux host** in v1 (Windows only — Apollo's virtual display is Windows-only).
- **No upstream Sunshine / Moonlight** in v1; their support is a later phase.
- **No router configuration.** Perigee explains router changes; it does not log into routers.
- **No modification of Artemis's private settings** (see §9.3 — not technically possible from outside its sandbox).

### 2.3 Users

| Persona | Setup | Main question |
|---|---|---|
| **Tablet player** | Windows gaming PC + Android tablet/phone over Wi-Fi | "Why does it stutter at 1440p?" |
| **Living-room player** | Windows PC + Android TV box (Shield, Chromecast w/ Google TV) | "Can I do 4K HDR on the TV?" |
| **Tinkerer** | Multiple clients, wired + wireless, tweaks Apollo config | "Which change actually helped?" |

---

## 3. Product surfaces

Reference design: Perigee design canvas (7 artboards). The spec below is the behavioral contract for those screens.

| Surface | Runs on | Screens (from design) | Input |
|---|---|---|---|
| **Desktop** | Apollo host, Windows 10/11 x64 | Overview (score, profile, breakdown, path diagram, latency chart, enhancements, all checks) · Network · Host · Client · History | Mouse/keyboard |
| **Android** | Artemis client, phone/tablet | Analyze (scanning) · Results · Fix detail · History · Settings | Touch |
| **Android TV** | Artemis client, TV box | Same flows, landscape 960×540dp, focus-driven | D-pad, OK/Back |

### 3.1 Core flow

```
Open app ─► Discover / pair peer ─► Run analysis (6 steps) ─► Results ─► Fix detail ─► Apply / follow guide ─► Re-test ─► History diff
```

Analysis steps (as in the Scan screens):

1. **Network link** — interface, band, channel, width, RSSI, PHY rate (or Ethernet speed)
2. **Find Apollo host** — mDNS discovery of the Perigee peer and of Apollo itself
3. **Latency & jitter** — 2,000 UDP probes, idle and under load
4. **Throughput at target bitrate** — 20 s UDP burst at the profile's bitrate (+20 % headroom)
5. **Decoder & display** — client codecs, decode latency, refresh rate, HDR
6. **Host checks** — encoder, encode latency, ports, power, GPU scheduling (from the desktop peer)

If no peer is paired, steps 3–4 run against a lightweight **reflector** (§6.4) or are marked *partial*, and the score is shown with a **reduced-confidence** badge.

---

## 4. Architecture

### 4.1 Stack

**Kotlin Multiplatform** with **Compose Multiplatform** UI.

| Target | Runtime | UI |
|---|---|---|
| Android phone/tablet | Android 8.0+ (API 26) | Compose |
| Android TV | Android TV 9+ (API 28) | Compose for TV (`androidx.tv`) |
| Windows desktop | JVM 21 (bundled via jpackage / Conveyor) | Compose Desktop |

Why KMP: the scoring engine, recommendation rules, peer protocol and data model are shared verbatim across all three targets, while platform probes stay native (`expect/actual`). This guarantees the phone and the PC compute the same score from the same data.

### 4.2 Module layout

```
perigee/
├─ core/
│  ├─ model/          # Measurement, Check, Score, Fix, Profile — pure Kotlin, serializable
│  ├─ scoring/        # scoring curves, category weights, caps, projection
│  ├─ rules/          # recommendation rules engine (data-driven, see §8)
│  ├─ profile/        # Artemis profile recommender (§9)
│  └─ protocol/       # peer protocol messages, codecs, versioning
├─ probe/
│  ├─ common/         # probe interfaces (expect), UDP probe engine, stats (p50/p95/p99)
│  ├─ android/        # WifiManager, ConnectivityManager, MediaCodecList, Display
│  └─ windows/        # WMI, netsh/WLAN API, DXGI/NVML/AMF/QSV, powercfg, registry, firewall
├─ peer/              # discovery (mDNS), pairing, TLS session, reflector
├─ storage/           # scan history (SQLDelight)
├─ ui/
│  ├─ shared/         # design tokens, score ring, breakdown, fix cards
│  ├─ android/        # phone/tablet navigation
│  ├─ tv/             # D-pad focus graph, TV layouts
│  └─ desktop/        # desktop shell, path diagram, charts
├─ host-helper/       # small elevated Windows helper for applying fixes (§10.2)
└─ app-android/ · app-tv/ · app-desktop/
```

### 4.3 Runtime topology

```
 ┌──────────── Windows host ────────────┐          ┌──────── Android / TV client ────────┐
 │ Apollo (Sunshine fork)               │          │ Artemis                             │
 │                                      │          │                                     │
 │ Perigee Desktop ──┐                  │  LAN     │                ┌── Perigee Android  │
 │   host probes     │  peer service ◄──┼──TLS/TCP─┼──► peer client │     client probes  │
 │   reflector (UDP) │◄────────────────┼──UDP────┼──► probe engine│                    │
 │ Perigee Helper (elevated, on demand) │          │                                     │
 └──────────────────────────────────────┘          └─────────────────────────────────────┘
```

---

## 5. Peer protocol

### 5.1 Discovery
- Desktop advertises `_perigee._tcp` via mDNS/DNS-SD with TXT `v=<protocol>`, `name`, `apollo=<bool>`.
- Android uses `NsdManager`; desktop uses JmDNS.
- Fallback: manual IP entry, and QR code shown on desktop.

### 5.2 Pairing
- Desktop shows a 6-digit code; Android user enters it (TV: on-screen number pad).
- Both sides generate a self-signed key pair on first launch; pairing exchanges and pins certificate fingerprints (SPAKE2 or code-authenticated ECDH).
- Paired peers are remembered; unpairing from either side revokes.

### 5.3 Transport
- **Control channel:** TLS 1.3 over TCP, default port **47100** (configurable; chosen to avoid Apollo's 47984–48010 range). Messages: length-prefixed CBOR (`kotlinx.serialization`).
- **Probe channel:** UDP, default port **47101**, sequence-numbered and timestamped packets; payload padded to match stream packet sizes (~1,400 B).
- Protocol version negotiated on connect; incompatible versions show an "update Perigee" prompt.

### 5.4 Messages (initial set)

| Message | Direction | Purpose |
|---|---|---|
| `Hello{version, device, role}` | both | handshake |
| `StartProbe{mode, durationMs, rateMbps, pktSize}` | client→host | begin latency or throughput run |
| `ProbeReport{stats}` | both | per-side stats (loss, reorder, one-way delay deltas) |
| `HostSnapshot{checks[]}` | host→client | host-side measurements |
| `ClientSnapshot{checks[]}` | client→host | client-side measurements |
| `ScanResult{score, fixes, profile}` | both | final shared result |
| `ApplyFix{fixId}` / `FixResult` | client→host | request a host-side fix (requires confirmation on the host) |

---

## 6. Measurements

Every measurement is a `Check{id, value, unit, budget, status: Pass|Warn|Fail|Unknown, source, confidence}`.

### 6.1 Network path

| Check | Android source | Windows source | Budget (Pass) |
|---|---|---|---|
| Link type | `ConnectivityManager` / `NetworkCapabilities` | `Get-NetAdapter` / WMI | Ethernet, or Wi-Fi 5/6 GHz |
| Wi-Fi band, channel, width | `WifiInfo.frequency`, `ScanResult.channelWidth` | WLAN API (`WlanQueryInterface`) | 5/6 GHz, ≥ 80 MHz |
| Signal (RSSI) | `WifiInfo.rssi` | WLAN API | ≥ −65 dBm |
| PHY / link rate | `WifiInfo.linkSpeed` / `rxLinkSpeed` | WLAN API / adapter speed | ≥ 2.5 × target bitrate |
| Wi-Fi standard | `WifiInfo.wifiStandard` (API 30+) | WLAN API | Wi-Fi 5+ |
| RTT median / p95 | UDP probe engine | UDP probe engine | ≤ 10 ms / ≤ 15 ms |
| Jitter p95 | UDP probe engine | UDP probe engine | ≤ 5 ms |
| Loss at target bitrate | UDP burst | UDP burst | ≤ 0.1 % |
| Sustained UDP throughput | UDP burst, 20 s | UDP burst | ≥ target × 1.2 |
| Latency under load (bufferbloat) | RTT during burst | RTT during burst | Δ ≤ 5 ms vs idle |
| Host uplink | — | adapter type, speed, duplex | Ethernet ≥ 1 Gbps |
| Hop count / same subnet | traceroute-lite (TTL probe) | same | 1 hop, same subnet |
| Router capabilities (WMM, band steering) | inferred (see §6.5) | inferred | — |

### 6.2 Host · Apollo (Windows)

| Check | Source | Budget |
|---|---|---|
| Apollo installed / running / version | service + process query, install path | running, supported version |
| Apollo ports reachable | TCP 47984, 47989, 48010; UDP 47998–48000 — tested from client side; Windows Firewall rules read via `INetFwPolicy2` | all open on LAN profile |
| GPU & encoder | DXGI adapter enumeration; NVENC via NVML/NVENC caps, AMF, QSV | HW HEVC; AV1 bonus |
| Encode latency | short synthetic encode via the vendor API (or Apollo log parse if available) | ≤ 5 ms at target res |
| Hardware-accelerated GPU scheduling | registry `HwSchMode` | On |
| Power plan | `powercfg /getactivescheme` | High / Ultimate performance while streaming |
| Game Mode | registry | On |
| GPU driver age | driver date | < 6 months |
| Apollo config sanity | Apollo config file (encoder, min/max bitrate, virtual display) | consistent with recommended profile |
| Host network adapter offloads / power saving | adapter advanced properties | "Energy-efficient Ethernet" off |

### 6.3 Client · Artemis (Android / TV)

| Check | Source | Budget |
|---|---|---|
| Artemis installed / version | `PackageManager` (package id to confirm, see §16) | installed |
| HW decoders | `MediaCodecList` — HEVC Main10, AV1, H.264; reject software codecs (`isSoftwareOnly`) | HW HEVC; AV1 bonus |
| Max decoder resolution × fps | `VideoCapabilities.areSizeAndRateSupported` | ≥ target profile |
| Decode latency | short decode of an embedded test clip with `MediaCodec`, timestamp in/out | ≤ 8 ms |
| Display refresh modes | `Display.getSupportedModes()` | ≥ target fps |
| HDR support | `Display.getHdrCapabilities()` | HDR10 for HDR profiles |
| Battery saver / thermal state | `PowerManager.isPowerSaveMode`, `getCurrentThermalStatus` | off / nominal |
| Low-latency Wi-Fi lock support | `WifiManager` (`WIFI_MODE_FULL_LOW_LATENCY`, API 29+) | supported |
| TV: Ethernet port present but unused | `NetworkCapabilities` + interface list | flag |

### 6.4 Measurement engine rules

- Probes run **idle** (RTT baseline) and **loaded** (during the throughput burst) to expose bufferbloat.
- Clock skew is removed by using RTT for latency and **inter-arrival deltas** for jitter (no clock sync required).
- Statistics: median, p95, p99, max; loss %, reorder %, burst-loss length.
- **Reflector mode:** when only the Android app is installed, the user can run the desktop app in *reflector-only* mode, or Perigee falls back to the default gateway (ICMP/UDP echo) and marks results as partial.
- Throughput tests cap at the target bitrate × 1.5 to avoid flooding the network unnecessarily, and abort if loss > 5 % for 2 s.
- Each scan stores raw summary stats, not packet traces.

### 6.5 Inferred checks

Some things cannot be read directly and are inferred, with lower confidence:
- **WMM off** — inferred when stream-class (DSCP-marked) probes see the same queuing as best-effort under load.
- **Band steering** — inferred when the client is on 2.4 GHz while a same-SSID 5 GHz BSSID with good RSSI is visible in scan results.
- **Channel congestion** — count and strength of overlapping BSSIDs on the client's channel.

Inferred checks are labelled "likely" in the UI and never by themselves produce a Fail.

---

## 7. Scoring model

### 7.1 Categories and weights

| Category | Max | What it covers |
|---|---|---|
| **Network path** | 35 | link type, band/width, signal, PHY rate, RTT, p95 jitter, loss, sustained UDP throughput, bufferbloat |
| **Host · Apollo** | 30 | encoder, encode latency, ports, HAGS, power plan, uplink, Apollo config |
| **Client · Artemis** | 20 | HW decode (HEVC Main10/AV1), decode latency, refresh rate, power/thermal |
| **Settings fit** | 15 | Artemis bitrate vs measured throughput, resolution/fps vs display, codec choice |

### 7.2 Sub-metric scoring

Each check maps to `[0, 1]` with a piecewise-linear curve between a **budget** (score 1.0) and a **floor** (score 0.0), e.g.:

| Metric | 1.0 at | 0.0 at |
|---|---|---|
| RTT median | ≤ 5 ms | ≥ 30 ms |
| Jitter p95 | ≤ 3 ms | ≥ 20 ms |
| Loss at target | ≤ 0.05 % | ≥ 2 % |
| Throughput ratio (sustained ÷ target) | ≥ 1.3 | ≤ 0.6 |
| Encode latency | ≤ 3 ms | ≥ 16 ms |
| Decode latency | ≤ 4 ms | ≥ 20 ms |

Category score = Σ (sub-score × sub-weight), sub-weights defined in `core/scoring/weights.json` and versioned.

### 7.3 Hard caps

Some failures make streaming fundamentally poor regardless of other results:

| Condition | Total score capped at |
|---|---|
| No hardware decoder for any supported codec | 39 |
| No hardware encoder on host | 39 |
| Apollo ports blocked | 39 |
| Loss at target bitrate > 2 % | 49 |
| Host on Wi-Fi *and* client on 2.4 GHz | 59 |

### 7.4 Bands

| Score | Verdict | Color token |
|---|---|---|
| 80–100 | **Excellent** | cyan `#47D7E8` |
| 60–79 | **Playable** | cyan `#47D7E8` |
| 40–59 | **Struggling** | amber `#F5A524` |
| 0–39 | **Not ready** | coral `#FF6F61` |

### 7.5 Projected score

`projected = score(measurements with every recommended fix's expected value applied)`. Each fix declares the measurements it changes and their expected post-fix values (e.g., "move to 5 GHz → band=5, width=80, PHY≥600, retransmits≤3 %"). Per-fix gain = score with that fix alone − current score; gains are shown individually and the total is computed jointly (not summed) to avoid double counting.

### 7.6 Confidence

Each scan carries `confidence: High | Medium | Low`:
- **High** — paired peers, all six steps completed.
- **Medium** — reflector or gateway fallback, or inferred checks contributing > 10 points.
- **Low** — steps skipped or failed.

Low-confidence scores are shown with a dashed ring and an explanation.

### 7.7 Versioning

The scoring model has its own version (`scoring/v1`). History entries record which version produced them; the UI warns when comparing across versions.

---

## 8. Recommendations engine

Rules are **data-driven** (YAML/JSON in `core/rules/`), so the community can add or tune them without code changes.

```yaml
id: net.wifi.move-to-5ghz
area: Network
when: client.link.type == "wifi" && client.wifi.band == 2.4
expect: { client.wifi.band: 5, client.wifi.width: 80, client.wifi.phy: 600, net.loss: 0.05 }
title: "Move the {device} to 5 GHz or 6 GHz"
why: "2.4 GHz is crowded and narrow. Each retransmit delays a video frame, which you feel as stutter."
steps:
  - "In the router settings, turn off band steering or give 5 GHz its own network name."
  - "Pick an 80 MHz channel (36–48 or 149–161) and join it from this {device}."
  - "Re-test. If signal is weaker than −65 dBm, move closer or add a wired access point."
action: { kind: open_wifi_settings }
unlocks_profile: true
```

### 8.1 Action kinds

| Kind | Where | Example |
|---|---|---|
| `guide` | any | router changes, cabling |
| `open_setting` | Android | Wi-Fi settings, battery saver, display |
| `apply_on_host` | Desktop (via Helper, confirmed) | enable HAGS, switch power plan, add firewall rule |
| `profile_change` | Artemis | lower bitrate to 40 Mbps (see §9.3) |

### 8.2 Initial rule set (v1)

Network: move to 5/6 GHz · use 80/160 MHz width · move closer / weak signal · plug in Ethernet (TV with unused port) · enable WMM · reduce channel congestion · host on Wi-Fi → wire it · bufferbloat → enable SQM/QoS.
Host: enable HAGS · High performance power plan while streaming · update GPU driver · open Apollo ports in firewall · disable Energy-Efficient Ethernet · enable Game Mode.
Client: turn off battery saver · enable high refresh rate · device thermally throttled.
Settings: bitrate above throughput · resolution above decoder/display capability · fps above display refresh · codec switch (H.264 → HEVC/AV1).

Fixes are sorted by projected gain, then by effort (`low | medium | high`).

---

## 9. Artemis profile recommender

### 9.1 Output
`Profile{ resolution, fps, codec, bitDepth, hdr, bitrateMbps }` — the "Best Artemis profile now" card, and the "Unlocks" profile shown after fixes.

### 9.2 Algorithm
1. **Codec:** best codec supported in hardware on both ends: AV1 > HEVC Main10 > HEVC > H.264.
2. **Candidate grid:** resolutions {720p, 1080p, 1440p, 4K, native} × fps {60, 90, 120, native refresh}, filtered by decoder capability and display modes.
3. **Bitrate need** per candidate from a codec-efficiency table (e.g., HEVC 1080p120 ≈ 40 Mbps, 1440p120 ≈ 80 Mbps, 4K60 HDR ≈ 100 Mbps — tunable in `profile/bitrates.json`).
4. **Feasible** if `need ≤ 0.8 × sustained throughput`, `loss@need ≤ 0.1 %`, encode + decode latency fit the frame budget (`1000 / fps` ms).
5. Choose the feasible candidate with the highest pixel rate, preferring higher fps at equal pixel rate for touch/handheld, higher resolution for TV.

### 9.3 Applying to Artemis — constraint

Android app sandboxing prevents Perigee from writing Artemis's settings. v1 handles "Apply to Artemis" by:
- showing the profile as a **copyable checklist** with deep-link "Open Artemis" (launch intent); and
- tracking an **upstream contribution**: propose an import intent or settings-file import to Artemis (GPL-3.0, open to PRs). If accepted, Perigee switches to one-tap apply.

The design's "Apply to Artemis" / "Send to Artemis" buttons should therefore read **"Open Artemis with these settings"** until upstream support exists.

Reading Artemis's *current* settings has the same constraint: v1 asks the user to confirm their current bitrate/resolution (one-time, remembered), or infers it from the desktop side if Apollo logs expose the session's requested parameters.

---

## 10. Platform specifics

### 10.1 Android
- Permissions: `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `INTERNET`, `CHANGE_WIFI_MULTICAST_STATE` (mDNS), `NEARBY_WIFI_DEVICES` (API 33+, `neverForLocation`) or `ACCESS_FINE_LOCATION` (API ≤ 32) for Wi-Fi scan results — explained before requesting; scan works without it at lower confidence.
- Keep screen on during scan; foreground service for the probe run.
- Use `WIFI_MODE_FULL_LOW_LATENCY` during probes to mirror Artemis behavior.

### 10.2 Windows
- Desktop app runs **unelevated**. Reading checks needs no admin rights.
- **Perigee Helper**: a small signed executable launched via UAC only when the user applies a host fix. It accepts a fixed allow-list of operations (`set_power_plan`, `set_hags`, `add_firewall_rule`, `revert`), logs each change, and stores the previous value for one-click revert.
- "Power plan while streaming" uses Apollo's client connect/disconnect commands where available, so the change applies only during sessions.
- Native access via JNA/JNR for WLAN API, DXGI, NVML and firewall COM; vendor encoder probes may live in a small native library (C++), built in CI.

### 10.3 Android TV
- Every interactive element is focusable with a visible 3 px focus ring (design token).
- Focus order defined explicitly per screen; Back always returns one level.
- Number-pad overlay for pairing codes.
- Leanback launcher banner; no touch-only gestures.

---

## 11. Data & storage

- **History** (SQLDelight, per device): scan id, timestamp, peer, scoring version, score, category scores, checks, fixes, profile.
- Synced between paired peers on request so both sides can show the same history.
- Export a scan as JSON (for bug reports and sharing) — anonymized by default (no SSIDs, MACs, IPs, hostnames).
- Retention: last 100 scans, user-clearable.

---

## 12. Privacy & security

- No network traffic leaves the LAN in normal operation. No analytics by default.
- Optional, opt-in anonymous result sharing (later phase) to calibrate scoring.
- Pairing is mutual and certificate-pinned; unpaired devices can only see the mDNS advertisement.
- The probe service accepts traffic only from paired peers and enforces rate caps.
- Helper allow-list is fixed at build time; no arbitrary command execution.

---

## 13. UX & design

- Source of truth: the Perigee design canvas.
- **Tokens:** background `#0A0D11`, surface `#11161D`, border `#232B36`, text `#E9EDF2`, muted `#94A0AF`, accent cyan `#47D7E8`, warn amber `#F5A524`, fail coral `#FF6F61`.
- **Type:** Chakra Petch (display, numbers), IBM Plex Sans (body), IBM Plex Mono (measurements).
- Status is never color-only: Pass/Warn/Fail always carry a text label.
- Contrast ≥ 4.5:1 for text; touch targets ≥ 44 dp; TV focus targets ≥ 48 dp.
- Localization: English and Brazilian Portuguese at v1.0; strings externalized from day one.

---

## 14. Quality & testing

| Layer | Approach |
|---|---|
| Scoring & rules | Pure unit tests with **golden scenario fixtures** (e.g., "tablet on 2.4 GHz, 80 Mbps set" → score 70 ± 1, fixes in expected order) |
| Probe engine | Tests against emulated networks: `clumsy` (Windows) and `tc netem` (Linux CI) injecting delay, jitter, loss |
| Platform probes | Device lab matrix: 1 phone, 1 tablet, NVIDIA Shield, Chromecast w/ Google TV, 1 NVIDIA + 1 AMD Windows host |
| Ground truth | Compare Perigee's verdict with a real Artemis session's on-screen stats (frame drops, network latency) for each lab config |
| UI | Compose screenshot tests per screen and per band; TV focus-traversal tests |

**Non-functional targets:** full scan ≤ 90 s; Android battery cost ≤ 2 % per scan; desktop idle CPU ≈ 0 % when not scanning; app size ≤ 25 MB (Android), ≤ 120 MB (desktop installer with JVM).

---

## 15. Distribution & licensing

- **License:** GPL-3.0 recommended for ecosystem alignment with Apollo and Artemis (both GPL-3.0). Perigee does not link their code, so a permissive license (Apache-2.0) is also possible — decide before first public commit.
- **Android:** GitHub Releases + Obtainium first; F-Droid after reproducible builds; Google Play optional.
- **Windows:** MSI/MSIX via jpackage or Conveyor, GitHub Releases, winget manifest; code signing via SignPath (free for OSS) or similar.
- CI: GitHub Actions — build, unit tests, netem tests, signed release artifacts.

---

## 16. Open questions

| # | Question | Needed by |
|---|---|---|
| Q1 | Artemis package id (`com.limelight.noir` per its Obtainium config) — confirm and whether a settings-import intent would be accepted upstream | Phase 4 |
| Q2 | Does Apollo expose session parameters (requested resolution/bitrate/codec) in logs or its web UI API that Perigee can read locally? | Phase 3 |
| Q3 | Most reliable way to measure host encode latency without disturbing a running Apollo (vendor SDK test encode vs. log parsing) | Phase 2 |
| Q4 | Bitrate-need table values — calibrate from lab sessions | Phase 3 |
| Q5 | Final peer ports (47100/47101 proposed) — check for conflicts with common software | Phase 1 |
| Q6 | Name "Perigee" — check trademark/app-store conflicts | before public release |
| Q7 | Apollo's release cadence and stability of config/log formats — the integration should tolerate changes | ongoing |

---

## 17. Glossary

- **Apollo** — Windows-focused fork of the Sunshine game-stream host, with built-in virtual display and client permissions.
- **Artemis** — Android fork of the Moonlight client, designed to pair with Apollo.
- **HAGS** — Hardware-accelerated GPU scheduling (Windows).
- **WMM** — Wi-Fi Multimedia, the Wi-Fi QoS mechanism that prioritizes latency-sensitive traffic.
- **Bufferbloat** — latency increase caused by oversized queues when the link is loaded.
- **Peer** — the other Perigee install (desktop ↔ Android) this device is paired with.
