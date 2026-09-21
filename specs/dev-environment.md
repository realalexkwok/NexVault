# NexVault — Dev Environment

> Constitution file. What must exist on the machine before any roadmap item can be
> verified. Mirrors the role `specs/1-page-resume/dev-environment.md` plays in the
> Byterra monorepo.
>
> Measured **2026-09-20** on the owner's Mac (Apple Silicon, macOS).

## 1. JDK

| Item | Value |
| --- | --- |
| JDK Android Studio now uses | **`temurin-21`** (Eclipse Adoptium / Temurin 21) — set by Studio during the AGP 9.1.0 → 9.4.1 upgrade, 2026-09-21. Recorded in the tracked `.idea/misc.xml`. |
| JBR (still the CLI convention) | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (JDK 25.0.3) |
| Where the Temurin 21 comes from | **Gradle downloaded it**, not Android Studio: `settings.gradle.kts` declares `org.gradle.toolchains.foojay-resolver-convention`, which provisioned it into `~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/` (335 MB extracted + a 190 MB tarball). It is **not** visible to `/usr/libexec/java_home`, which is why it looks absent. |
| Also on the machine | Homebrew `openjdk` 25 at `/opt/homebrew/opt/openjdk` (what bare `java` resolves to), Homebrew `openjdk@17`, Temurin 16, Microsoft 11, Corretto 11, Corretto 8 |
| Project Java level | 17 (`sourceCompatibility` / `jvmTarget`) |

**Both JDKs build this project.** JDK 21 and JDK 25 each produced a green
`:app:assembleDebug` and a green unit suite during the 2026-09-21 regression pass, and both
can target Java 17. JDK 21 is the more conservative choice — it is an LTS and it is what
Android Studio selected — so prefer it in the IDE; the CLI recipe below stays as it is
because it is what `.cursor/rules/shell-commands.mdc` and every recorded command use.

The repository's standing convention (`.cursor/rules/shell-commands.mdc`) is to run
Gradle with the Android Studio JBR:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <task>
```

`.vscode/settings.json` pins the same path via `java.import.gradle.java.home`, so VS Code
and the CLI agree — **but Android Studio now disagrees with both**, using Temurin 21. The
split is harmless (verified above) yet worth knowing before blaming a build failure on the
JDK.

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
| Installed platforms | `android-33`, `android-34`, `android-35`, `android-36`, `android-36.1`, **`android-37.2`** — compileSdk needs **37.2** ✅ |
| Build tools | `35.0.0`, `36.0.0`, `36.1.0`, **`37.0.0`** ✅ |
| Platform tools | **37.0.1** (`adb` 1.0.41 / 37.0.1-15733141), `fastboot` |
| Command-line tools | `cmdline-tools/latest/bin/{sdkmanager,avdmanager,apkanalyzer,retrace,…}` ✅ |
| `emulator` package | ❌ not installed — **not required** (owner uses a physical device) |
| System images | `android-25;google_apis`, `android-29;default`, `android-30;{google_apis,aosp_atd,default,google_apis_playstore}`, `android-33;{google_apis,default,google_apis_playstore,android-desktop}`, `android-36.1;google_apis_playstore;arm64-v8a` |
| Configured AVDs | none (`~/.android/avd` is empty) — not required |
| Connected device | ✅ **Pixel 6a** (`bluejay`), Android 16 / API 36, over adb Wi-Fi |

Toolchain raised to SDK 37 on 2026-09-21 (owner-approved). The 36.x packages are still
installed, so a rollback needs no download.

**`targetSdk` 37 vs an API-36 device:** the project targets 37 but the only test device
runs 36. That is legal and installs fine — it simply means Android 17's runtime behaviour
changes are not exercised by anything here. Tracked as Phase 4 item 4.15.

## 3. Device setup for the manual verification half

**Owner decision 2026-09-21: use a physical Android device over USB, not the emulator.**
Installing the emulator is therefore **not** a prerequisite and nothing needs downloading.

**The device is currently LOCKED**, which is what still blocks the manual half:

```
$ adb shell dumpsys window | grep isKeyguardShowing
    isKeyguardShowing=true
$ adb shell dumpsys trust | grep trustState
    trustState=UNTRUSTED, deviceLocked=1
```

`adb shell input keyevent KEYCODE_WAKEUP` turns the screen on and
`adb shell wm dismiss-keyguard` does **not** clear a secure lock. The owner must enter
their PIN/pattern; `screencap` returns an all-black frame until then.

To confirm the device is usable:

```bash
SDK=/Users/superguo/Library/Android/sdk
"$SDK/platform-tools/adb" devices        # must list the device as "device", not "unauthorized"
"$SDK/platform-tools/adb" shell dumpsys window | grep isKeyguardShowing   # want: false
```

A physical device is also the *better* target here: biometric unlock needs real
fingerprint hardware, which the AOSP emulator images do not emulate.

**No item can be marked `[x]` right now**, because the manual half of two-sided
verification has never been performed: `adb devices` lists nothing, and the app has never
been launched since the project began.

To unblock it, attach a device with USB debugging enabled and confirm:

```bash
SDK=/Users/superguo/Library/Android/sdk
"$SDK/platform-tools/adb" devices        # must list your device as "device", not "unauthorized"
```

A physical device is also the *better* target here: biometric unlock needs real
fingerprint hardware, which the AOSP emulator images do not emulate.

### If an emulator is ever wanted instead

The `android-36.1;google_apis_playstore;arm64-v8a` image is already on disk and matches
`targetSdk 36`; only the emulator package and an AVD definition would be missing:

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

## 8. Remote build host — `superguo-SQM2270`

Added 2026-09-21. **Why it exists:** the Mac has **8 GiB of RAM**, and running the node
harness, Gradle and Android Studio at once triggered repeated out-of-memory dialogs during
development. The remote has 15 GiB. This is a capacity fix, not a configuration fix — there
was nothing wrong with the Mac's Gradle setup.

### The host

| Item | Value |
| --- | --- |
| SSH alias | `superguo-SQM2270` (already in `~/.ssh/config`; user and key come from there) |
| OS / kernel | Ubuntu 24.04.4 LTS, kernel 7.0.0 |
| Architecture | **x86_64** — *not* the Mac's aarch64; see "why nothing was copied" below |
| CPU / RAM | 4 cores / 15 GiB (≈8 GiB available) |
| Disk | ~307 GB free |
| Network | `192.168.1.17` (enp1s0), **`10.42.0.1` (wlo1 hotspot)** — the phone pairs over the 10.42.0.x range |
| `sudo` | **requires a password** — the whole setup below was done without it |

### Installed layout

| Item | Path | Version |
| --- | --- | --- |
| Project | `~/Projects/NexVault` | `main` @ the same commit as the Mac |
| Android SDK root | `~/Android/Sdk` | — |
| cmdline-tools | `~/Android/Sdk/cmdline-tools/latest` | build 16111833 |
| platforms | `~/Android/Sdk/platforms/android-37.2` | matches `compileSdk` |
| build-tools | `~/Android/Sdk/build-tools/37.0.0` | matches the Mac |
| platform-tools | `~/Android/Sdk/platform-tools` | 37.0.1 |
| JDK | `/usr/lib/jvm/java-17-openjdk-amd64` | OpenJDK **17**.0.20 |

### Why nothing was copied from the Mac

The architectures differ (x86_64 vs aarch64), and Android SDK `build-tools` /
`platform-tools` are **platform-specific binaries** — a straight copy would produce a
broken SDK. Everything was installed fresh from Google's repositories instead, and the
project came from GitHub (`git clone`), which already had the exact `main` the Mac had.

**No new JDK was needed.** AGP 9.4.1's class files are compiled to major version **61 =
Java 17**, so the distro's existing OpenJDK 17 runs the build. This is what kept the whole
setup sudo-free: `openjdk-21-jdk` is available from apt, but installing it would have
required a password.

### Reproducing the setup

```bash
ssh superguo-SQM2270
git clone https://github.com/superguo/NexVault.git ~/Projects/NexVault

# Android SDK, no sudo required
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
SDK=~/Android/Sdk
mkdir -p "$SDK/cmdline-tools" && cd /tmp
curl -LO https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
unzip -q commandlinetools-linux-16111833_latest.zip -d /tmp/cmdtools
mv /tmp/cmdtools/cmdline-tools "$SDK/cmdline-tools/latest"
SM="$SDK/cmdline-tools/latest/bin/sdkmanager"
yes | "$SM" --licenses
yes | "$SM" "platform-tools" "platforms;android-37.2" "build-tools;37.0.0"
```

`local.properties` (git-ignored, so it must be created by hand) points at the Linux SDK:

```properties
sdk.dir=/home/superguo/Android/Sdk
INFURA_API_KEY=your_key_here
# …the same five placeholder keys as the Mac
```

### Building on the remote

```bash
ssh superguo-SQM2270
cd ~/Projects/NexVault
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ulimit -n 32768            # see caveat 1
./gradlew :app:assembleDebug
```

`adb` and `sdkmanager` are on `PATH` via `~/.bashrc` (backup: `~/.bashrc.bak-nexvault`).
**They only resolve in an interactive shell** — Ubuntu's `.bashrc` returns early for
non-interactive shells, so `ssh host 'adb devices'` needs
`export PATH="$HOME/Android/Sdk/platform-tools:$PATH"` first.

### Verified on 2026-09-21

| Check | Result |
| --- | --- |
| `git -C ~/Projects/NexVault log -1 --format=%h` | matches the Mac's `main` |
| `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** in 7m 14s (cold, incl. Gradle 9.6.0 download) |
| APK | 43 MB — identical to the Mac's clean build |
| `./gradlew testDebugUnitTest :domain:test` | **223 tests, 222 passing, 1 skipped, 0 failing** |
| Per-module test counts | identical to the Mac, module for module |
| `adb devices` | Pixel 6a (bluejay), Android 16 / API 36, over adb TLS |
| `sudo` prompts during setup | **zero** |

### Caveats specific to this host

1. **`ulimit -n` is 1024**, low for an Android build. Raise it per session with
   `ulimit -n 32768`; the hard limit is 1048576 so no privilege is needed. Persisting it
   would need `/etc/security/limits.conf`, i.e. `sudo`.
2. **No Android udev rules are installed** (`/etc/udev/rules.d/` holds only snap rules).
   The account is in `plugdev`, which is not sufficient on its own. **Wireless debugging
   works and needs no rules**; USB would require an admin-installed rule file.
3. **`~/.gradle` is shared with other projects** on this host, so nothing was written
   there — no global JDK or JVM-args setting was set, to avoid changing their builds.
4. The `repo` and `local.properties` are the only project-local additions; **no repository
   file was modified for the remote**. `git status` is clean there.
5. `.idea/` is tracked and therefore present on the remote with macOS paths and a `jbr-25`
   reference. Harmless — there is no Android Studio on the host to read them.
6. The newer cmdline-tools prints its package IDs with a **`/`** separator
   (`platforms/android-37.2`) rather than the historical `;`, and warns that `sdkmanager`
   is superseded by `android sdk`. The `;` form still installs correctly.
7. The build emits one deprecation worth tracking: `hiltViewModel` has moved to
   `androidx.hilt.lifecycle.viewmodel.compose`. It will need handling alongside any future
   `hilt-navigation-compose` bump — see roadmap 4.16.

