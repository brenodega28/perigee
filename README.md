# Perigee

Perigee tells anyone who streams games with Apollo and Artemis, in under two minutes, how ready their setup is — as one honest number from 0 to 100 — and exactly what to change to make it better.

Self-hosted game streaming fails in ways that are hard to see: a tablet quietly parked on 2.4 GHz, a bitrate set above what the link can carry, a host on a Balanced power plan, a TV with battery-saver-style refresh caps. Perigee replaces trial, forum threads and guesswork with measurement.

Perigee is a Kotlin Multiplatform project with a Compose Multiplatform UI, targeting Android and Windows desktop. It is licensed under GPL-3.0.

## Requirements

- JDK 21
- Android SDK (command-line tools, platform and build-tools) with `sdk.dir` set in `local.properties`

See [AGENTS.md](AGENTS.md) for the exact environment setup.

## Commands

```
./gradlew check                       # build, unit tests, ktlint, Android Lint
./gradlew ktlintFormat                # auto-fix formatting
./gradlew :app-android:assembleDebug  # Android debug APK
./gradlew :app-desktop:run            # run the desktop app
./gradlew :app-desktop:jar            # desktop jar
./gradlew projects                    # list modules
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).
