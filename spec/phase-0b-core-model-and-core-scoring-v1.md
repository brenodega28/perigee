---
title: Phase 0b — core/model and core/scoring v1
status: in-progress
owner: Breno
linked_runs: [60]
created: 2026-10-29
---

## Summary

Implement the shared data model (`:core:model`) and the v1 scoring engine (`:core:scoring`) from SPEC §6 and §7 (all of §7: curves, weights, caps, bands, confidence, projection), as pure Kotlin in `commonMain`, and prove it with golden scenario fixtures. The main fixture is the design canvas's "tablet on 2.4 GHz, Artemis at 1440p120 HEVC 80 Mbps" scenario, which must score exactly 70 (Network 18/35, Host 25/30, Client 18/20, Settings 9/15, Playable), project to 91, and rank the six fixes +11, +4, +2, +2, +1, +1 in the design's order. Sub-weights live in a versioned `weights.json` (`scoring/v1`), embedded at build time and parsed with kotlinx.serialization. The engine takes measurements, current Artemis settings and a list of fix expectations as input; deciding which fixes apply is the rules engine's job (Phase 4), so fixtures supply the fixes. The first commit cherry-picks 21a3d33 (SPEC.md, ROADMAP.md) from `crew/lead/lead`.

## Interfaces

- `:core:model` (package `brenodega28.perigee.core.model`), all `@Serializable`: `MetricId` (one entry per scored metric in SPEC §6.1–6.3 that §7.1 lists), `MeasuredValue` (number with unit, boolean, or enum such as band/link type/codec), `Measurements` (map MetricId → MeasuredValue; a missing metric means not measured), `Status { Pass, Warn, Fail, Unknown }`, `Category { Network, Host, Client, Settings }`, `Check { id, value, unit, budget, status, source, confidence }` (SPEC §6), `ArtemisSettings { resolution, fps, codec, bitDepth, hdr, bitrateMbps }`, `Effort { Low, Medium, High }`, `FixExpectation { id, category, effort, changes: Measurements, settingsChange: ArtemisSettings? }`, `CategoryScore { category, points: Int, max: Int }`, `Verdict { Excellent, Playable, Struggling, NotReady }`, `Confidence { High, Medium, Low }`, `ScanContext { paired: Boolean, completedSteps: Set<AnalysisStep>, usedFallback: Boolean }`, `ScanScore { scoringVersion, total, categories, verdict, appliedCap, confidence, checks }`, `FixGain { fixId, gain }`, `Projection { projectedTotal, gains: List<FixGain> }`.
- `:core:scoring` (package `brenodega28.perigee.core.scoring`): `ScoringEngine(weights: ScoringWeights = ScoringWeights.v1())` with `fun score(measurements: Measurements, settings: ArtemisSettings, context: ScanContext): ScanScore` and `fun project(measurements: Measurements, settings: ArtemisSettings, context: ScanContext, fixes: List<FixExpectation>): Projection`.
- `Curve`: piecewise-linear map from a value to [0, 1] between budget (1.0) and floor (0.0), either direction, clamped; the six curves in SPEC §7.2 plus builder-chosen curves for the other metrics.
- `ScoringWeights` parsed with kotlinx.serialization from `weights.json` in `:core:scoring`: `version` (`scoring/v1`), category maxima (35/30/20/15), per-metric sub-weights, curve budgets/floors, Pass/Warn/Fail thresholds, hard caps, band edges.
- Build: a Gradle task in `:core:scoring` (or a build-logic convention) generates a Kotlin source holding the contents of `weights.json` into a generated `commonMain` source dir, wired so editing the JSON triggers regeneration. kotlinx.serialization plugin and runtime are added to `gradle/libs.versions.toml` at current stable versions.
- Rounding rule: each category's points are rounded to an Int; `total` = sum of rounded categories, then the lowest matching hard cap (SPEC §7.3) is applied; verdict from SPEC §7.4 bands.
- Projection rule (SPEC §7.5): projected = score with every fix's changes applied jointly; per-fix gain = score with that fix alone − current score; gains ordered by gain desc, then effort (Low < Medium < High), then input order.
- Confidence rule (SPEC §7.6): High when paired and all six steps completed; Medium on fallback, or when inferred checks (WMM, band steering, congestion) contribute > 10 points; Low when any step is skipped or failed. Inferred checks never produce Fail on their own (SPEC §6.5).
- Fixtures under `core/scoring/src/commonTest/` (Kotlin builders or JSON), one per scenario, each recording its inputs and the expected outputs.

## Out of scope

- Rules engine and rule YAML/JSON (`:core:rules`, Phase 4): fixtures hand fix expectations to the engine directly.
- Profile recommender (`:core:profile`, Phase 3): the "Best profile now" / "Unlocks" cards are not computed here.
- Probes, peer protocol messages, storage, UI and design tokens (Phase 0c).
- The Android TV (Shield) scenario as a golden fixture: its "raise bitrate once wired" fix conflicts with SPEC §7.5 and its HAGS gain differs from the tablet's; the design gets fixed later.
- Calibration of curves against real Artemis sessions (Phase 3, Q4): v1 weights are documented assumptions.

## Acceptance criteria

- [ ] The branch starts with commit 21a3d33 from `crew/lead/lead` cherry-picked (adds specs/SPEC.md and specs/ROADMAP.md).
- [ ] `:core:model` and `:core:scoring` are pure Kotlin in `commonMain` with no platform code, and build for JVM and Android; the placeholder `CoreModel`/`CoreScoring` objects and their tests are replaced.
- [ ] `weights.json` exists in `:core:scoring`, carries `"version": "scoring/v1"`, is embedded by a Gradle task at build time and parsed with kotlinx.serialization, and is the only source of sub-weights, curve endpoints, thresholds, caps and bands; `ScanScore.scoringVersion` is `scoring/v1`.
- [ ] Tablet fixture inputs come from the design canvas: client Wi-Fi 2.4 GHz ch 6, 20 MHz, −58 dBm, 144 Mbps PHY, 18 % retransmits; RTT median 6.8 ms; jitter p95 11.2 ms; loss 0.8 % at 80 Mbps; sustained UDP 61 Mbps; WMM off (inferred), band steering on (inferred); host Ethernet 1 Gbps full duplex, Apollo TCP 47984/47989/48010 and UDP 47998–48000 open, NVENC HEVC 10-bit + AV1, encode 3.1 ms, HAGS off, Balanced power plan; client HW HEVC Main10 + AV1, decode 4.4 ms, 60 Hz refresh (battery saver on); Artemis 1440p · 120 fps · HEVC · 80 Mbps; paired, all six steps done. Any value the design doesn't show is chosen by the builder and listed in the fixture as an assumption.
- [ ] Tablet fixture scores exactly Network 18/35, Host 25/30, Client 18/20, Settings 9/15, total 70, verdict Playable, no cap applied, confidence High.
- [ ] Tablet fixture check statuses match the design's All checks list: Host uplink, Apollo TCP ports, Apollo UDP ports, Host encoder, Encode latency, Client decoder, Decode latency = Pass; Client Wi-Fi band = Fail; HAGS, Power plan, Client refresh rate = Warn. RTT median = Pass; jitter p95, loss at target and sustained UDP = Warn.
- [ ] Tablet fixture projection with the six design fixes (move to 5 GHz, lower bitrate to 40 Mbps, enable WMM [Low], enable HAGS [Medium], High performance power plan, battery saver off) gives projected total 91 and gains in order: 5 GHz +11, bitrate +4, WMM +2, HAGS +2, power plan +1, battery saver +1. The 5 GHz fix's expected values match the Fix screen: 5 GHz · 80 MHz, PHY ≥ 600 Mbps, retransmits < 3 %, sustained UDP ≥ 100 Mbps, jitter p95 ≤ 5 ms.
- [ ] Each hard cap in SPEC §7.3 has a fixture or test that triggers it and shows the total capped at 39/39/39/49/59, with `appliedCap` set; when several apply, the lowest wins.
- [ ] A wired, all-pass fixture scores ≥ 80 (Excellent) with no fixes.
- [ ] Tests cover band edges (39/40, 59/60, 79/80), each confidence level, curve clamping on both ends, and a missing metric (status Unknown, scored without crashing; documented whether it scores 0 or is excluded and re-normalised).
- [ ] Fix-gain ordering ties break by effort then input order, as tested by the tablet fixture (WMM Low before HAGS Medium; power plan before battery saver by input order).
- [ ] The chosen sub-weights and every assumed value are documented next to `weights.json` (a short markdown file, not code comments).
- [ ] ROADMAP.md Phase 0 item "core/model and core/scoring v1 with golden scenario fixtures" is ticked.
- [ ] `./gradlew check` passes; no code comments added.

## Verification

On a clean checkout with JDK 21 and the Android SDK from AGENTS.md: run `./gradlew check` and confirm it passes. Run `./gradlew :core:scoring:jvmTest` and open the test report to confirm the tablet golden test asserts 18/25/18/9 = 70, Playable, High, projected 91 and the gain order +11/+4/+2/+2/+1/+1. Then change one sub-weight in `weights.json`, rerun, and confirm the golden test fails, proving the weights are actually read from the file. A checker repeats this on a fresh checkout and compares the 11 check statuses against the design canvas's All checks list.
