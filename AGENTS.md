# Agent instructions

Only what an agent can't work out from the code. Keep this under a page; enforce anything that matters with a check or a hook instead.

## Commands

One-time environment setup (macOS, Homebrew):

```
brew install openjdk@21
brew link --force openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
brew install --cask android-commandlinetools
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-36" "build-tools;36.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

`local.properties` is gitignored and must contain `sdk.dir`. macOS ships a `/usr/bin/java` stub that fails, so `JAVA_HOME` must point at JDK 21 before running `./gradlew`.

- Check (build + unit tests + ktlint + Android Lint): `./gradlew check`
- Format: `./gradlew ktlintFormat`
- Android debug APK: `./gradlew :app-android:assembleDebug`
- Run desktop app: `./gradlew :app-desktop:run`
- Desktop jar: `./gradlew :app-desktop:jar`
- Windows CI tasks: `./gradlew :app-desktop:build :probe:windows:check`
- List modules: `./gradlew projects`

## Conventions

- No code comments in source or build scripts.
- Kotlin packages are `brenodega28.perigee.<module path>`; the Android applicationId is `brenodega28.perigee`.
- Versions live in `gradle/libs.versions.toml`; shared build logic lives in `build-logic/` convention plugins.

## Gotchas

- `:core`, `:probe` and `:ui` show up in `./gradlew projects` as empty container projects; Gradle creates them for colon paths.
