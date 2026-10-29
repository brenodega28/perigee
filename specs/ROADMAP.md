# Perigee — Roadmap

Companion to [SPEC.md](SPEC.md) · drafted 2026-10-28

**Assumptions:** one developer, part-time (~10–15 h/week), open source from day one. Estimates are in calendar weeks at that pace and include testing. Starting early **November 2026**, v1.0 lands around **June–July 2027**.

The order is chosen so that something useful ships early (Android alone gives a partial score), the riskiest technical piece (accurate UDP measurement) is proven first, and the two halves meet in the middle.

```
Nov 26        Jan 27        Mar 27        May 27        Jul 27        Sep 27
|── P0 ─|── P1 Android ──|── P2 Desktop ──|── P3 Pair ──|─ P4 Fix ─|─P5 TV─|─P6 1.0─|── P7 Sunshine/Moonlight ──► P8+
          ▲ 0.1 preview                    ▲ 0.3 alpha   ▲ 0.5 beta          ▲ 1.0
```

---

## Phase 0 — Foundations · 2–3 weeks

Set up the project so every later phase plugs into it.

- [x] Repo, license decision (SPEC §15), CONTRIBUTING, code of conduct
- [x] KMP project skeleton: `core/*`, `probe/*`, `ui/*`, `app-android`, `app-desktop` (SPEC §4.2)
- [ ] Design tokens + shared Compose components: score ring, category bar, check row, fix card
- [ ] `core/model` and `core/scoring` v1 with **golden scenario fixtures** (the design's "tablet on 2.4 GHz" scenario must score 70)
- [ ] GitHub Actions: build all targets, unit tests, lint
- [ ] Resolve Q5 (peer ports)

**Exit:** CI green on Android + desktop; scoring engine reproduces the design's example scores from fixtures.

---

## Phase 1 — Android analyzer, standalone · 5–6 weeks → **v0.1 preview**

Prove the measurement engine on the client side, without a desktop peer.

- [ ] Wi-Fi/link probes: band, channel, width, RSSI, PHY, Wi-Fi standard, Ethernet detection
- [ ] Decoder & display probes: `MediaCodecList` HW codecs, size/rate support, refresh modes, HDR, battery saver, thermal
- [ ] Decode-latency micro-benchmark with embedded test clips (H.264 / HEVC Main10 / AV1)
- [ ] UDP probe engine (shared `probe/common`): RTT, jitter (inter-arrival), loss, reorder, stats
- [ ] Gateway fallback probing → **partial score, Low/Medium confidence**
- [ ] Inferred checks: band steering, channel congestion (SPEC §6.5)
- [ ] Screens: Scan, Results (partial), Fix detail for client + network rules
- [ ] Probe engine validated under `tc netem` in CI

**Exit:** on a real phone, the Results screen shows Network + Client categories with correct Pass/Warn/Fail against a lab setup.
**Release:** v0.1 APK on GitHub + Obtainium — "client-side readiness check", invite early testers.

---

## Phase 2 — Windows desktop host analyzer · 5–6 weeks

The host half, usable on its own as a host health check.

- [ ] Host probes: adapter/uplink, GPU + encoder caps (NVENC, AMF, QSV), HAGS, power plan, Game Mode, driver age, EEE
- [ ] Apollo detection: install, version, running state, config file parse
- [ ] Windows Firewall rule inspection for Apollo ports
- [ ] Encode-latency measurement — resolve **Q3** with a spike first (week 1)
- [ ] Reflector mode (UDP echo) so the Android app can run full latency/throughput tests against the host
- [ ] Desktop shell: Overview layout with Host tab; network path diagram (static data)
- [ ] Windows installer via jpackage/Conveyor; unsigned dev builds in CI

**Exit:** desktop app shows a correct Host · Apollo score on one NVIDIA and one AMD machine; Android ↔ reflector gives full network measurements.

---

## Phase 3 — Pairing & end-to-end score · 5–6 weeks → **v0.3 alpha**

Join the halves. This is the first time the full 0–100 score exists.

- [ ] mDNS discovery, 6-digit pairing, certificate pinning, TLS control channel (SPEC §5)
- [ ] Throughput test at target bitrate, loaded RTT / bufferbloat, DSCP-marked WMM inference
- [ ] Host ↔ client snapshot exchange; identical `ScanResult` on both sides
- [ ] Full scoring with hard caps, bands, confidence (SPEC §7)
- [ ] **Profile recommender** (SPEC §9) + "Unlocks" profile
- [ ] Projected score from fix expectations
- [ ] Desktop: latency chart, path diagram with live values, All checks table
- [ ] Calibration sessions: run real Artemis streams on 4–6 lab configs; tune the bitrate table and curves (resolves **Q4**); investigate **Q2** (Apollo session params)

**Exit:** for every lab config, Perigee's recommended profile streams without visible stutter, and its verdict matches the real session.
**Release:** v0.3 alpha (Android + Windows), feedback form for score vs. real experience.

---

## Phase 4 — Fixes & actions · 4–5 weeks → **v0.5 beta**

Turn findings into changes.

- [ ] Data-driven rules engine with the full v1 rule set (SPEC §8.2) and per-fix gain
- [ ] Perigee Helper (elevated, allow-listed): power plan, HAGS, firewall rule, **revert**
- [ ] "Power plan only while streaming" via Apollo connect/disconnect commands
- [ ] Android `open_setting` actions (Wi-Fi, battery saver, display)
- [ ] "Open Artemis with these settings" checklist + launch
- [ ] **Upstream PR to Artemis** proposing a settings-import intent (resolves **Q1**; merge is outside our control — tracked, not blocking)
- [ ] Re-test flow and before/after diff on Results
- [ ] Code signing for Windows (SignPath application submitted in Phase 2)

**Exit:** applying the top three fixes in a lab config moves the re-tested score to within ±5 of the projection.
**Release:** v0.5 beta.

---

## Phase 5 — Android TV · 3–4 weeks

- [ ] `app-tv` module with Compose for TV, Leanback banner
- [ ] TV layouts for Scan, Results, Fix detail (960×540 dp, from the design)
- [ ] Explicit focus graph, 3 px focus ring, number-pad pairing overlay, Back behavior
- [ ] TV-specific rules: unused Ethernet port, 4K HDR feasibility, display mode matching
- [ ] Test on NVIDIA Shield and Chromecast with Google TV

**Exit:** every action on every TV screen is reachable with D-pad only; TV focus traversal tests pass.

---

## Phase 6 — v1.0 hardening & release · 3–4 weeks → **v1.0**

- [ ] Scan history with peer sync, compare view, JSON export (anonymized)
- [ ] Brazilian Portuguese translation; English copy review
- [ ] Accessibility pass: contrast, TalkBack labels, non-color status
- [ ] Performance targets: scan ≤ 90 s, battery ≤ 2 %/scan (SPEC §14)
- [ ] Docs site: how scoring works, every check and its budget, troubleshooting
- [ ] Name/trademark check (**Q6**)
- [ ] Distribution: GitHub Releases, Obtainium, winget manifest; start F-Droid reproducible build submission
- [ ] Announce in the Apollo/Artemis community (GitHub Discussions, relevant subreddits/Discord)

**Exit:** v1.0 meets every goal G1–G8 in SPEC §2.1.

---

## Phase 7 — Sunshine & Moonlight support · 3–4 weeks → **v1.1**

- [ ] Detect upstream Sunshine on Windows; adapt config parsing and checks
- [ ] Detect Moonlight (Android) alongside Artemis; per-client profile output
- [ ] Rules that differ by host (e.g., virtual display availability) gated on host type
- [ ] Rename scoring categories in UI: "Host · Sunshine", "Client · Moonlight" where relevant

---

## Phase 8+ — Later

Ordered by expected value; revisit after v1.0 feedback.

| Idea | Notes |
|---|---|
| **Remote / WAN readiness** | NAT type, UPnP, IPv6, uplink bandwidth, path to host from mobile data; Tailscale/ZeroTier detection |
| **Linux host** (Sunshine/Apollo) | VAAPI/NVENC probes, `tc`-based QoS hints |
| **Opt-in anonymous result sharing** | calibrate curves and bitrate table from real-world data |
| **Continuous monitor mode** | lightweight background probe during real sessions; flags when conditions degrade |
| **Router integrations** | read-only APIs (OpenWrt/ubus, ASUSWRT) for exact WMM/band-steering state |
| **iOS / Apple TV clients** | if Artemis-equivalent clients exist there |
| **Multi-client households** | detect concurrent streams and household traffic competing for airtime |

---

## Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Measured network ≠ real stream behavior | score loses trust | Calibration against real Artemis sessions in Phase 3; show confidence; ground-truth feedback form |
| Artemis upstream won't add settings import | no one-tap apply | Checklist + launch is the v1 baseline; feature is nice-to-have |
| Apollo config/log format changes | host checks break | Version-detect Apollo; tolerant parsers; checks degrade to *Unknown*, not *Fail* |
| Encode-latency probe interferes with a live Apollo session | bad UX | Skip test encode while a session is active; use cached value |
| Android Wi-Fi API restrictions (location/nearby permissions, OEM quirks) | missing checks | Request with clear rationale; degrade to lower confidence; device lab coverage |
| Windows code signing / SmartScreen friction | install drop-off | Apply to SignPath early (Phase 2); winget distribution |
| Solo bandwidth | schedule slip | Each phase ships something usable; Phases 5 and 7 can move after v1.0 if needed |

---

## Milestone summary

| Version | Phase end | What users get |
|---|---|---|
| **0.1** | P1 | Android client-side readiness check (partial score) |
| **0.3 alpha** | P3 | Full 0–100 score with paired desktop + Android, recommended profile |
| **0.5 beta** | P4 | Ranked fixes, one-click host fixes with revert, re-test diff |
| **1.0** | P6 | Android TV, history, pt-BR, signed releases |
| **1.1** | P7 | Sunshine & Moonlight support |
