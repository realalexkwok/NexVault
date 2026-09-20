# NexVault — Dev Environment

> Constitution file. What must exist on the machine before any roadmap item can be
> verified. Mirrors the role `specs/1-page-resume/dev-environment.md` plays in the
> Byterra monorepo.
>
> Measured **2026-09-20** on the owner's Mac (Apple Silicon, macOS).

## 1. JDK

| Item | Value |
| --- | --- |
| Known-good JDK | Android Studio JBR — `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (JDK 25.0.3) |
| Also on the machine | Homebrew `openjdk` 25 at `/opt/homebrew/opt/openjdk` (this is what bare `java` resolves to), Homebrew `openjdk@17`, Temurin 16, Microsoft 11, Corretto 11, Corretto 8 |
| Project Java level | 17 (`sourceCompatibility` / `jvmTarget`) |

The repository's standing convention (`.cursor/rules/shell-commands.mdc`) is to run
Gradle with the Android Studio JBR:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <task>
```

`.vscode/settings.json` pins the same path via `java.import.gradle.java.home`, so the
editor and the CLI agree.

The stated historical reason for the override — "the bundled JDK lacks `jlink`" — refers
to the **Cursor Java extension's** bundled runtime, not to Homebrew's. Both the JBR and
Homebrew `openjdk` 25 do ship `bin/jlink`. The override is therefore kept as the
repository's known-good, reproducible choice rather than as a workaround for a broken
JDK; if you switch JDKs, re-run E1/E2 in `roadmap.md`'s evidence appendix to prove the
build still works.

## 2. Android SDK

| Item | Value |
| --- | --- |
| SDK root | `/Users/superguo/Library/Android/sdk` (from `local.properties` → `sdk.dir`) |
| Installed platforms | `android-33`, `android-34`, `android-35`, `android-36`, `android-36.1` — compileSdk needs **36** ✅ |
| Build tools | `35.0.0`, `36.0.0`, `36.1.0` ✅ |
| Platform tools | present (`adb`, `fastboot`) |
| Command-line tools | `cmdline-tools/latest/bin/{sdkmanager,avdmanager,apkanalyzer,retrace,…}` ✅ |
| `emulator` package | ❌ **NOT INSTALLED** |
| System images | `android-25;google_apis`, `android-29;default`, `android-30;{google_apis,aosp_atd,default,google_apis_playstore}`, `android-33;{google_apis,default,google_apis_playstore,android-desktop}`, **`android-36.1;google_apis_playstore;arm64-v8a`** |
| Configured AVDs | ❌ none (`~/.android/avd` is empty) |
| Connected devices | ❌ none (`adb devices` lists nothing) |

## 3. The emulator gap — prerequisite for every manual verification

**No item can be marked `[x]` right now**, because the manual half of two-sided
verification is physically impossible: there is no emulator binary, no AVD, and no
connected device. The app has never been launched since the project began.

The good news is that the expensive part is already on disk — the
`android-36.1;google_apis_playstore;arm64-v8a` image matches `targetSdk 36` and is an
Apple-Silicon build. Only the emulator package and an AVD definition are missing:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
SDK=/Users/superguo/Library/Android/sdk

# 1. Install the emulator binary (network download)
"$SDK/cmdline-tools/latest/bin/sdkmanager" --install emulator

# 2. Create an AVD from the already-downloaded image (Pixel 7 profile)
"$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
  -n NexVault_API36 \
  -k "system-images;android-36.1;google_apis_playstore;arm64-v8a" \
  -d pixel_7

# 3. Boot it and wait for the device
"$SDK/emulator/emulator" -avd NexVault_API36 &
"$SDK/platform-tools/adb" wait-for-device
```

Notes:
- Prefer a `google_apis_playstore` image over `aosp`/`default` for biometric testing —
  the AOSP images have no fingerprint hardware, and unlock is a core flow here.
- `sdkmanager` prints a harmless `test: : integer expression expected` line from its own
  shell wrapper; it does not affect the install.
- An alternative to the emulator is a physical Android device over USB with USB
  debugging enabled — that also gives a truthful answer about real biometric hardware.

## 4. API keys

`local.properties` (git-ignored) feeds `app/build.gradle.kts` BuildConfig fields:

| Property | Used for | Where to get one |
| --- | --- | --- |
| `INFURA_API_KEY` | Ethereum/Sepolia RPC | infura.io (free tier) |
| `ALCHEMY_API_KEY` | Alternative RPC | alchemy.com (free tier) |
| `COINGECKO_API_KEY` | Fiat prices, price history | coingecko.com (free/demo tier) |
| `ETHERSCAN_API_KEY` | Transaction + token-transfer history | etherscan.io |
| `WALLETCONNECT_PROJECT_ID` | WalletConnect v2 (Phase 3.1) | cloud.walletconnect.com |

**All five are currently placeholders** (`your_key_here`). Expected consequences until
real keys exist: the home dashboard has no balances or fiat values, the chart has no data
series, token detail shows no price, and history refresh returns nothing. These are
**expected degradations, not bugs** — do not paper over them with fake data. Roadmap item
2.0.4 owns the decision of whether to supply real keys or to formalise key-absent
behaviour.

Never commit real keys. Never log mnemonic, private key, or PIN material.

## 5. Command reference

All from the repository root.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# --- build ---
./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug           # install onto a running emulator/device

# --- test ---
./gradlew testDebugUnitTest           # Android library/application modules
./gradlew :domain:test                # domain is a pure-Kotlin JVM module
./gradlew :core:core-security:testDebugUnitTest   # currently fails to compile (2.0.1)

# --- quality gates ---
./gradlew detekt                      # config: config/detekt/detekt.yml, maxIssues 0
./gradlew ktlintCheck                 # no .editorconfig; ktlint 1.x defaults

# --- reports ---
# test results: <module>/build/test-results/testDebugUnitTest/TEST-*.xml
# ktlint:       <module>/build/reports/ktlint/ktlint*Check/ktlint*Check.txt
# detekt:       <module>/build/reports/detekt/detekt.{txt,html,md,sarif}
```

The quality reports above are the evidence source for `validation.md` — cite the
absolute path, not a summary.

`spotless` technically runs but has no configuration, so `spotlessCheck` proves nothing;
do not use it as a gate until Phase 4.6 either configures it or removes it.

## 6. Pre-verification checklist

Before claiming any roadmap item is verified, confirm:

- [ ] An emulator or physical device is attached (`adb devices` is non-empty)
- [ ] `./gradlew :app:assembleDebug` succeeds and the APK path is recorded
- [ ] The relevant `testDebugUnitTest` / `:domain:test` task passes
- [ ] `detekt` and `ktlintCheck` are green (only possible from Phase 2.0.5 onward)
- [ ] Absolute artifact paths and raw command output are in `validation.md`
- [ ] The owner has walked the flow on the device and reported the result

## 7. Environment facts verified on 2026-09-20

| Check | Command | Observed |
| --- | --- | --- |
| JBR is a full JDK | `"$JAVA_HOME/bin/java" -version` | `openjdk 25.0.3` |
| JBR ships `jlink` | `ls "$JAVA_HOME/bin/jlink"` | present |
| Homebrew JDK ships `jlink` too | `ls /opt/homebrew/Cellar/openjdk/25/.../bin/jlink` | present |
| Bare `java` resolves to | `which java` | `/opt/homebrew/opt/openjdk/bin/java` |
| Build works **without** the override | `env -u JAVA_HOME ./gradlew :data:clean :data:assembleDebug` | **BUILD SUCCESSFUL** — `:data:compileDebugKotlin` executed for real (not UP-TO-DATE) |
| `sdkmanager` runs | `sdkmanager --version` | `21.0` (prints a cosmetic `test: : integer expression expected` from its own wrapper) |
| `avdmanager` runs | `avdmanager` | usage text; `--version` unsupported |
| compileSdk 36 platform present | `ls platforms` | `android-36`, `android-36.1` |
| Emulator binary absent | `ls "$SDK/emulator"` | No such file or directory |
| No AVD defined | `ls ~/.android/avd` | empty |
| No device attached | `adb devices` | empty list |

**Conclusion on the `JAVA_HOME` override:** it is not strictly required — `:data`
compiles under the Homebrew JDK 25 as well. Keep using the JBR anyway: it is the JDK
Android Studio itself runs, it is what `.vscode/settings.json` pins, and it keeps CLI and
editor builds reproducible. If the JDK ever changes, re-run E1 and E2 from `roadmap.md`'s
evidence appendix before trusting a build result.

