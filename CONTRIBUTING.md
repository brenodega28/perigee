# Contributing to Perigee

## Setup

Install JDK 21 and the Android SDK, then create `local.properties` with `sdk.dir`. The exact steps are in [AGENTS.md](AGENTS.md).

## Build, test and lint

```
./gradlew check                       # build + unit tests + ktlint + Android Lint
./gradlew ktlintFormat                # auto-fix formatting
./gradlew :app-android:assembleDebug  # Android debug APK
./gradlew :app-desktop:run            # run the desktop app
```

`./gradlew check` must pass before you open a pull request. CI runs it on every push and pull request to `main`.

## Branch and pull request flow

1. Branch from `main`, using a short descriptive name such as `feature/score-ring` or `fix/udp-timeout`.
2. Keep commits focused, with clear messages.
3. Run `./gradlew check` locally.
4. Open a pull request against `main` and wait for CI to pass.
5. Address review feedback with follow-up commits.

## Code comments

Do not add comments to code. This covers inline `//` comments, block comments and KDoc, in source files and build scripts. Make the code explain itself through naming and structure instead. If something genuinely cannot be understood without a comment, raise it in the pull request first.

## Code style

ktlint enforces formatting for every module. Rules live in `.editorconfig`.

## License

By contributing you agree that your contributions are licensed under GPL-3.0.
