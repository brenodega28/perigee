---
title: "Phase 0a — KMP skeleton, CI and repo docs"
status: in-progress
owner: Breno
linked_runs: [58]
created: 2026-10-28
---

## Summary

First slice of ROADMAP Phase 0. Turns the empty repo into a Kotlin Multiplatform + Compose Multiplatform project with the module layout from SPEC §4.2, so every later phase has somewhere to plug in. Adds GPL-3.0 licensing, CONTRIBUTING and a Contributor Covenant 2.1 code of conduct, plus a GitHub Actions workflow that builds the Android and desktop targets, runs unit tests and lints. The modules are empty or placeholders: no scoring, probes or real UI yet. Those come in Phase 0b (core/model + core/scoring with golden fixtures) and Phase 0c (design tokens + shared Compose components).

## Interfaces

- Gradle wrapper (`./gradlew`), Kotlin DSL, version catalog `gradle/libs.versions.toml` pinned to current stable Kotlin, Compose Multiplatform, AGP; JVM toolchain 21; convention plugins in `build-logic/` for KMP library / Android app / desktop app modules
- Modules (SPEC §4.2): `:core:model`, `:core:scoring`, `:core:rules`, `:core:profile`, `:core:protocol`, `:probe:common`, `:probe:android`, `:probe:windows`, `:ui:shared`, `:ui:android`, `:ui:desktop`, `:app-android`, `:app-desktop`. `app-tv`, `ui/tv`, `peer`, `storage` and `host-helper` are added in their own phases
- Targets: core/* and probe/common are KMP (android + jvm); probe/android is Android-only; probe/windows is JVM-only; ui/shared is Compose Multiplatform (android + jvm)
- Kotlin root package `brenodega28.perigee` (modules use sub-packages, e.g. `brenodega28.perigee.core.model`); `:app-android` applicationId `brenodega28.perigee`, minSdk 26, a single Compose activity showing a placeholder screen that uses a composable from `:ui:shared`
- `:app-desktop`: Compose Desktop `main()` opening a window with the same placeholder composable; `./gradlew :app-desktop:run` works
- Each core module has at least one trivial unit test in `commonTest` so the test pipeline runs for real
- Lint: ktlint (or Spotless+ktlint) for all modules, plus Android Lint on `:app-android`; one Gradle task `./gradlew check` runs build + tests + lint
- `.github/workflows/ci.yml`: runs on push and PR to main; ubuntu runner builds Android debug APK + desktop jar and runs `./gradlew check`; windows runner builds `:app-desktop` and `:probe:windows` and runs their tests; Gradle caching enabled
- Repo docs: `LICENSE` (GPL-3.0 full text), `CONTRIBUTING.md` (build/test commands, branch/PR flow, no-code-comments convention), `CODE_OF_CONDUCT.md` (Contributor Covenant 2.1), `README.md` (one-paragraph mission from SPEC §1 + build commands), `.gitignore` for Gradle/Android/IDE/macOS (`.DS_Store`)
- `AGENTS.md` Commands section filled in with the exact build, lint, test and run commands, including any local SDK setup (JAVA_HOME / ANDROID_HOME / local.properties) an agent needs

## Out of scope

- Any scoring, model types, probes, peer protocol or real screens (Phases 0b, 0c and later)
- Design tokens and shared components (Phase 0c)
- Android TV module, host-helper, peer, storage modules
- Installers (jpackage/Conveyor), code signing, release workflows
- netem probe tests in CI (Phase 1)
- Creating a GitHub remote or pushing; CI is validated statically only
- Resolving Q5 (peer ports): the Lead researches this separately

## Acceptance criteria

- [ ] `./gradlew check` passes locally from a clean clone (build + unit tests + ktlint + Android Lint)
- [ ] `./gradlew :app-android:assembleDebug` produces an APK with applicationId `brenodega28.perigee`; `./gradlew :app-desktop:run` opens a window showing the placeholder screen
- [ ] `./gradlew projects` lists exactly the 13 modules in Interfaces, with the folder layout from SPEC §4.2
- [ ] core/* and probe/common compile for both android and jvm targets; `:probe:windows` is jvm-only, `:probe:android` is android-only
- [ ] Each core module's trivial test runs and passes on jvm (visible in the test report)
- [ ] A deliberate ktlint violation makes `./gradlew check` fail (checked by the builder, then reverted)
- [ ] `.github/workflows/ci.yml` passes `actionlint` and runs the same tasks as local `check` on ubuntu, plus desktop build/tests on windows
- [ ] LICENSE is the GPL-3.0 text; CODE_OF_CONDUCT.md is Contributor Covenant 2.1 with a contact placeholder; CONTRIBUTING.md and README.md list the real commands
- [ ] AGENTS.md Commands section lists the exact commands, and they work
- [ ] No code comments are added to source files (user convention)
- [ ] Versions of Kotlin, Compose Multiplatform, AGP and Gradle are current stable releases, pinned in the version catalog

## Verification

The builder installs JDK 21 and the Android command-line SDK locally (the user approved this). From a clean checkout it runs `./gradlew check`, `./gradlew :app-android:assembleDebug`, `./gradlew :app-desktop:run` (window appears) and `./gradlew projects`, plus a deliberate lint violation to confirm it fails. It runs actionlint on the workflow. A checker then repeats the commands on a fresh checkout of the branch and reviews the module layout against SPEC §4.2. There is no GitHub remote yet, so CI is not executed in this run.
