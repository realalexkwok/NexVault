# NexVault — Tech Stack

> Constitution file. Approved modules, libraries, versions, conventions and quality
> gates. **Never add a dependency that is not listed here**; adding one is a
> constitution amendment and belongs in a feature spec first.
>
> Versions below were read out of the actual build on **2026-09-20** from
> `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` and
> `app/build.gradle.kts`. They supersede `doc/02-ARCHITECTURE-AND-TECH-STACK.md`,
> which is stale in several places (see §Drift corrections).

## 1. Language & toolchain

| Item | Value | Source |
| --- | --- | --- |
| Language | Kotlin 2.3.10 (100% Kotlin, no Java sources) | `libs.versions.toml` |
| Build system | Gradle **9.6.0** (wrapper) + Kotlin DSL + version catalog | `gradle-wrapper.properties` |
| Android Gradle Plugin | **9.4.1** | `libs.versions.toml` |
| KSP | 2.3.6 | `libs.versions.toml` |
| compileSdk | **37.2** (`version = release(37) { minorApiLevel = 2 }`) | 19 module build files |
| minSdk | 26 | `app/build.gradle.kts` |
| targetSdk | **37** | `app/build.gradle.kts` |
| Java / JVM target | 17 (`sourceCompatibility` / `jvmTarget`) | `app/build.gradle.kts` |
| Compose compiler | `org.jetbrains.kotlin.plugin.compose` (Kotlin 2.x plugin) | root `build.gradle.kts` |
| Convention plugins | **none** — every module repeats its `android { }` block; there is no `build-logic/` module (see §Known gaps) | `settings.gradle.kts` |

Raising `targetSdk` to 37 is a **behavioural** change, not just a build setting: it opts
the app into Android 17 runtime restrictions. Neither the build nor the unit tests prove
anything about that behaviour, and the only available test device runs API 36. Tracked as
Phase 4 item 4.15.

Running any Gradle task requires the Android Studio JBR. (The historical reason given was
that Homebrew's JDK lacks `jlink`; that is not true of Homebrew's openjdk 25 — the claim
refers to the Cursor extension's bundled runtime. Both have `jlink`, and
`:data:assembleDebug` was verified to succeed without the override. Keep using the JBR
for reproducibility with Android Studio.)
See `dev-environment.md` for the exact command form.

## 2. Modules and dependency rules

20 modules, declared in `settings.gradle.kts`:

```
app
core/core-common  core/core-ui  core/core-network
core/core-database  core/core-datastore  core/core-security
domain            (pure Kotlin JVM — the only non-Android module)
data
feature/feature-onboarding  feature-home  feature-tokens  feature-send
feature/feature-receive  feature-history  feature-dapp  feature-nft
feature/feature-swap  feature-settings  feature-auth
```

**Verified dependency edges** (from each module's `build.gradle.kts`):

| Module | Depends on |
| --- | --- |
| `domain` | nothing (Kotlin stdlib, coroutines, `javax.inject` only) |
| `core/core-common` | nothing |
| `core/core-security` | nothing |
| `core/core-database` | nothing |
| `core/core-datastore` | `core:core-security` |
| `core/core-ui` | `domain` |
| `core/core-network` | `domain`, `core:core-common` |
| `data` | `domain` + all six `core:*` |
| `feature-onboarding` / `feature-home` / `feature-tokens` | `domain`, `core:core-common`, `core:core-ui` |
| `feature-auth` | `domain`, `core:core-common`, `core:core-ui`, `core:core-security`, `core:core-datastore` |
| `app` | every module above |

**Rules (binding):**

1. `domain` stays pure Kotlin/JVM: no Android SDK types, no Compose, no Retrofit, no
   Room. It holds models, repository *interfaces*, and use cases.
2. A `feature:*` module MUST NOT depend on `data`, on `app`, or on another `feature:*`.
   Features reach the domain through use cases only. (Verified: none currently do.)
3. A `feature:*` module depends on `domain` + `core:core-ui` + `core:core-common`, and
   may add `core:core-security` / `core:core-datastore` only when the screen genuinely
   needs them — `feature-auth` is the precedent.
4. Only `data` implements repository interfaces, and it binds them in
   `data/src/main/java/com/nexvault/wallet/data/di/RepositoryModule.kt`.
5. `core:*` modules MAY depend on `domain` (both `core-ui` and `core-network` do today).
   They must not depend on `data`, `app`, or any `feature:*`.
6. `app` owns DI wiring, the root navigation graph, and nothing else of substance.

## 3. Architecture & conventions

Clean Architecture with MVVM at the presentation layer and unidirectional data flow in
every Compose screen:

```
Compose screen → ViewModel (StateFlow<XxxUiState>) → UseCase → Repository interface (domain)
                                                              ↑ implemented by data
                 data → core-network (Retrofit/Web3j) · core-database (Room)
                      · core-datastore (DataStore) · core-security (KeyStore/Tink)
```

- **Result type.** `DataResult<out T>` in
  `domain/src/main/java/com/nexvault/wallet/domain/model/common/DataResult.kt` with
  exactly two variants — `Success<T>(data)` and `Error(exception, message?)`. There is
  **no `Loading` variant** (the archive's version had one; the code never did). Helpers:
  `map`, `flatMap`, `getOrNull`, `getOrThrow`, and `Result<T>.toDataResult()`.
  Repository methods return `DataResult<T>` or `Flow<T>`; they never throw to callers.
- **Use cases.** One class per action, `operator fun invoke(...)`, constructor-injected,
  in `domain/usecase/<area>/`: 20 exist today across `auth` (5), `chain` (3), `token` (8)
  and `wallet` (4).
- **Repositories.** 5 interfaces in `domain/repository/`: `AuthRepository`,
  `ChainRepository`, `TokenRepository`, `TransactionRepository`, `WalletRepository`.
- **ViewModels.** One `XxxViewModel` + one `XxxUiState` data class per screen, exposed as
  `StateFlow`, collected with `collectAsStateWithLifecycle()`.
- **DI.** Hilt everywhere; constructor injection, `@Singleton` on repository
  implementations, `@Inject lateinit var` only in Android entry points
  (`MainActivity`, workers).
- **Packages.** `com.nexvault.wallet.<layer>.<area>` — e.g.
  `com.nexvault.wallet.core.security.encryption`,
  `com.nexvault.wallet.feature.tokens`.
- **Compose.** Material 3; shared components and design tokens live in `core:core-ui`
  (`NexVaultButton`, `NexVaultCard`, `TokenIcon`, `PriceChangeChip`, `SimpleLineChart`,
  `ShimmerPlaceholder`, …). Dark theme is the default and currently the only theme.
- **KDoc.** Public classes and functions carry KDoc; `detekt`'s
  `comments/CommentOverPrivateFunction` is active.

## 4. Approved libraries

> **Approved upgrade, 2026-09-21.** The versions below are the target state for the
> grouped dependency upgrade, written here **before** the build files were touched. Each
> group is applied and regression-tested on its own. Verified against Google Maven, Maven
> Central, the Gradle Plugin Portal and JitPack on 2026-09-21.
>
> **Group 1** KSP · **Group 2** low-risk minor/patch · **Group 3** Compose BOM ·
> **Group 4** web3j 6 (major) · **Group 5** catalog hygiene.

### The Kotlin ↔ KSP constraint (hard)

`kotlin` **stays on 2.3.10**. KSP has published **zero 2.4.x releases** — its newest
version is 2.3.12 — and Hilt, Room and Moshi all rely on KSP code generation here, so
moving Kotlin to 2.4.20 would break `kspDebugKotlin` outright. `ksp` may advance within
the 2.3 line (2.3.6 → 2.3.12) because both ends of the pair stay on Kotlin 2.3.

Re-check this pairing before any future Kotlin bump: KSP publishes per-Kotlin-version, and
"a newer Kotlin exists" never implies "KSP supports it".

### Jetpack / AndroidX

| Library | Version | Use |
| --- | --- | --- |
| Compose BOM (G3) | **2026.09.00** (was 2026.03.00) | UI toolkit (Material 3) |
| `androidx.compose.material3` + `material-icons-extended` | via BOM | Components, icons |
| `androidx.navigation:navigation-compose` (G2) | **2.10.1** (was 2.9.7) | Navigation graphs |
| `androidx.hilt:hilt-navigation-compose` (G2) | **1.4.0** (was 1.3.0) | `hiltViewModel()` |
| `androidx.lifecycle:*` (G2) | **2.11.0** (was 2.10.0) | ViewModel, `collectAsStateWithLifecycle` |
| Hilt (G2) | **2.60.1** (was 2.59.2) | DI |
| Room (G2) | **2.8.5** (was 2.8.4) | Local database (`core-database`, schema JSON in `core/core-database/schemas/`) |
| DataStore Preferences | 1.2.1 | Settings, wallet metadata, secure app state — 1.3.0-alpha11 exists; stay on stable |
| WorkManager (G2) | **2.11.2** (was 2.11.1) | Background sync (Phase 3, not yet used) |
| CameraX | 1.5.3 | QR scanning — 1.6.2 available but the `camerax` bundle is referenced by nothing; see §4d |
| Biometric | **1.4.0-alpha07** (was 1.4.0-alpha05) | Unlock. Already on the alpha line; the stable line (1.1.0) is older than what the code uses |
| `androidx.core:core-ktx` (G2) | **1.19.0** (was 1.18.0) | Core extensions |
| AppCompat (G2) | **1.8.0** (was 1.7.1) | Theme base |

### Network & blockchain

| Library | Version | Use |
| --- | --- | --- |
| Retrofit | 3.0.0 | REST clients (CoinGecko, Etherscan-compatible explorers) — already latest |
| OkHttp + logging-interceptor (G2) | **5.5.0** (was 5.3.2) | HTTP, cache-control interceptor |
| Moshi (`moshi-kotlin-codegen` via KSP) | 1.15.2 | JSON — already latest |
| kotlinx-serialization-json (G2) | **1.11.0** (was 1.10.0) | Backup serialization |
| kotlinx-coroutines (G2) | **1.11.0** (was 1.10.2) | Structured concurrency |
| Web3j (`org.web3j:core`) (G4) | **6.0.0** (was 5.0.2) | JSON-RPC, ABI, transaction signing — **major bump, own spec** |
| WalletConnect Android BOM | 1.35.2 | DApp sessions (Phase 3) — already latest |
| kethereum `bip39` | 0.86.0 | BIP-39 / BIP-32 / BIP-44 — already latest on JitPack |

**G4 carries real risk.** `MnemonicManager` calls `MnemonicUtils.generateMnemonic`,
`validateMnemonic`, `generateSeed` **and** `getWords()` — the last one is the fix that made
`core-security`'s suite pass at all. A web3j 6 API change lands in the security module, so
this group is applied last, on its own, and must not be bundled with anything else.

API surface in `core:core-network`: `CoinGeckoApi`, `BlockExplorerApi` +
`BlockExplorerApiFactory`, `Web3jProvider`, `ChainConfigProvider`/`ChainNetworkConfig`,
`AddressValidator`, `BigDecimalAdapter`/`BigIntegerAdapter`, `CacheControlInterceptor`,
`CoinGeckoApiKeyInterceptor`.

### Security

| Library / API | Version | Use |
| --- | --- | --- |
| AndroidKeyStore | platform | Hardware-backed key wrapping |
| Google Tink (`tink-android`) (G2) | **1.23.0** (was 1.20.0) | AEAD primitives over KeyStore |
| kethereum bip39 | 0.86.0 | Mnemonic generation/validation |
| SQLCipher | — | **Not adopted.** Room is unencrypted today; deferred to Phase 4 |

Implemented in `core:core-security`: `EncryptionManager`, `KeyStoreManager`,
`MnemonicManager`, `HDKeyManager`, `WalletStore`, `BiometricHelper`,
`PasswordValidator`, `SecurityUtils`.

### UI, media, tooling

| Library | Version | Use |
| --- | --- | --- |
| Coil Compose | 2.7.0 | Image loading — already latest |
| Lottie Compose | 6.7.1 | Animated illustrations — already latest |
| Vico Compose (G2) | **3.3.1** (was 3.0.3) | Portfolio charts |
| ML Kit barcode-scanning | 17.3.0 | QR scanning — already latest |
| ZXing | **3.5.4** (was an orphan alias) | QR generation — see §4d |
| Accompanist permissions / systemuicontroller | 0.37.3 | Permissions, system bars — already latest |
| Shimmer | **0.5.0** (was a non-existent 1.2.0) | Loading placeholders — see §4d |
| LeakCanary (`debugImplementation`) | 2.14 | Leak detection — already latest stable (3.0 is alpha) |

### 4d. Catalog hygiene (Group 5)

Findings from the 2026-09-21 sweep. All were latent: nothing referenced the broken entries,
so nothing failed.

| Finding | Detail | Resolution |
| --- | --- | --- |
| **`shimmer = "1.2.0"` does not exist** | `com.facebook.shimmer:shimmer` tops out at **0.5.0** on Maven Central. With zero references it never failed — the first person to write `implementation(libs.shimmer)` would have hit an unresolvable dependency. | Pin the real latest (0.5.0) or delete the entry |
| **`zxing` is an orphan version alias** | A `[versions]` entry with **no matching library entry at all**; `libs.zxing` cannot resolve. Real latest is 3.5.4. | Add the library entry or delete the alias |
| **`compose-compiler = "1.5.12"` is dead config** | Under Kotlin 2.x the Compose compiler comes from `org.jetbrains.kotlin.plugin.compose`. `composeOptions.kotlinCompilerExtensionVersion` in `app` and `core:core-ui` is inert. | Delete both the property and the two `composeOptions` blocks |
| **4 unused bundles** | `blockchain`, `camerax`, `testing`, `debug` are declared and referenced by nothing; only `compose`, `compose.testing`, `compose.navigation`, `networking` are used. | Keep if Phase 3 will use them, else delete |
| **3 redundant library entries** | `detekt`, `ktlint`, `spotless` library aliases are unused — those tools are applied as plugins via `libs.plugins.*`. | Delete the library aliases |
| **`junit-jupiter 6.0.3`** | Unused; latest is 6.1.3. See roadmap 4.5. | Decide: adopt or delete |
| **`kotlinx-collections-immutable 0.4.0`** | Unused; latest 0.5.2. | Decide: adopt or delete |

## 5. Testing

| Library | Version | Status |
| --- | --- | --- |
| JUnit 4 | 4.13.2 | **The actual standard** — every module uses `testImplementation(libs.junit)`; already latest |
| MockK (G2) | **1.14.11** (was 1.14.9) | **The mocking library.** Mockito is not a dependency anywhere |
| kotlinx-coroutines-test (G2) | **1.11.0** (was 1.10.2) | `runTest`, `advanceUntilIdle` |
| Turbine | 1.2.1 | Flow assertions — already latest |
| Truth | 1.4.5 | Fluent assertions — already latest |
| Robolectric (G2) | **4.17** (was 4.16.1) | JVM Android tests (declared, not yet used) |
| `androidx.compose.ui:ui-test-*` (G3) | **1.12.1** (was 1.10.5) | Compose UI tests — moves with the BOM |
| `hilt-android-testing` (G2) | **2.60.1** (was 2.59.2) | DI-aware tests — tracks the Hilt version |
| JUnit 5 (`junit-jupiter` 6.0.3 → 6.1.3 available) | — | **Catalog-only, unused.** Either adopt or delete — open decision in Phase 4 |

Test layout: `<module>/src/test/java/...` mirrors the main source package.
Instrumented tests live in `app/src/androidTest/`.

**Measured state (2026-09-20; `core-security` row updated 2026-09-21 by 2.0.1)** — see
`roadmap.md` Phase 2.0 for the remediation plan:

| Module | Tests | Result |
| --- | --- | --- |
| `domain` | 63 | pass |
| `feature-onboarding` | 30 | pass |
| `core-datastore` | 27 | pass |
| `data` | 27 | pass |
| `feature-auth` | 14 | pass |
| `app` | 1 | pass (template stub) |
| `core-security` | 61 | pass (1 skipped) — **compiled and ran for the first time on 2026-09-21**; was 64 tests that never built |
| `core-network`, `core-database`, `core-ui`, `feature-home`, `feature-tokens` | 0 | no tests at all |

Repository total: **223 tests, 222 passing, 1 skipped, 0 failing** (2026-09-21).

`doc/07-TEST-CASES.md` defines 52 test cases (TC-SEC 8, TC-SECTEST 5, TC-UC 4, TC-REPO 5,
TC-NET 3, TC-DB 3, TC-VM 10, TC-UI 10, TC-INT 4). Nothing maps a TC id to a test method
today; building that traceability is Phase 2.0.6.

## 6. Quality gates

| Tool | Version | Config | Current result |
| --- | --- | --- | --- |
| Detekt | 1.23.8 — **already the latest published; no upgrade exists** | `config/detekt/detekt.yml` (`build.maxIssues: 0`, `maxLineLength: 120`, `TooManyFunctions thresholdInFiles: 11`, `weights.complexity: 2`) | **75 issues → fail** |
| ktlint | 14.2.0 (plugin) — **already the latest published; no upgrade exists** | no `.editorconfig` | **1565 violations → fail** |
| Spotless | **8.10.2** (G2, was 8.3.0) | **applied with no configuration block** | no-op — never cite it as evidence |

**Both gates are already on their newest available releases**, which has a consequence for
roadmap 4.9: the Gradle 10 deprecation (`ReportingExtension.file`, raised by the Detekt
plugin) **cannot be fixed by upgrading** — no newer detekt plugin exists to upgrade to. It
waits on an upstream release. (Maven Central tops out at detekt 1.23.8; the Gradle Plugin
Portal tops out at ktlint-gradle 14.2.0.)

All three are applied to every subproject from the root `build.gradle.kts`
(`subprojects { apply(...) }`); no module opts out. There is **no CI** — gates only run
when someone runs them locally.

Detekt's largest contributors: `TooManyFunctions` (`TokenRepositoryImpl` 19/11,
`WalletRepositoryImpl` 14/11, `AuthRepositoryImpl` 13/11) and `MaxLineLength`.
ktlint's: `multiline-expression-wrapping` 468, `function-signature` 306,
`argument-list-wrapping` 150, `trailing-comma-on-call-site` 113,
`function-expression-body` 106, `annotation` 92, `function-naming` 76,
`no-unused-imports` 56.

The ktlint situation is a **style-vocabulary conflict**, not sloppiness per rule: the
code follows IntelliJ/Android Studio defaults (annotations on the same line, 4-space
continuation) while ktlint 1.x defaults disagree. Phase 2.0.5 owns the decision between
reformatting the tree once and codifying the existing style in `.editorconfig` + ruleset
adjustments. Whichever is chosen, it lands as its own isolated change.

## 7. Secrets & configuration

`local.properties` (git-ignored) is read by `app/build.gradle.kts` and exposed as
BuildConfig fields:

| Property | Consumer |
| --- | --- |
| `INFURA_API_KEY` | RPC endpoints |
| `ALCHEMY_API_KEY` | RPC endpoints |
| `COINGECKO_API_KEY` | Fiat prices, price history |
| `ETHERSCAN_API_KEY` | Transaction / token-transfer history |
| `WALLETCONNECT_PROJECT_ID` | WalletConnect v2 (Phase 3) |

`sdk.dir` also lives there. **All five keys are currently `your_key_here` placeholders** —
network-backed screens are expected to show errors/empty states until real keys exist.
Faking data to make such a screen "work" is forbidden.

Never commit a key; never log a mnemonic, private key, or PIN; never write key material
to disk unencrypted.

## 8. Drift corrections (archive → reality)

| `doc/` says | Reality |
| --- | --- |
| targetSdk 34 | **36** |
| Kotlin "2.0+" | **2.3.10** |
| `DataResult` has a `Loading` variant | Only `Success` / `Error` |
| Tree contains `build-logic/` convention plugins | **Does not exist**; modules repeat their config |
| `core-common` holds utilities/constants/result wrappers | **Empty module** (0 Kotlin files) yet depended on by `app`, `data` and every feature |
| Branching strategy includes `develop` | Only `main` exists; trunk-based in practice |
| Compose UI Test 1.10.4 | **1.10.5** |
| ktlint 14.1.0 | **14.2.0** (plugin; ktlint 1.x rule engine) |
| `plans/project-scaffolding-plan.md` (Kotlin 2.0.21) | Stale; superseded by this file |
| Testing is "JUnit 5" | JUnit 4 is what modules actually use; JUnit 5 is unused |

## 9. Known gaps

- **`build-logic/` does not exist.** 20 modules duplicate their Android/Compose/JVM
  configuration. Extracting convention plugins is Phase 4 work.
- **`core-common` is empty** but wired into `app`, `data` and all features. Either give
  it the utilities it was meant to hold or remove it from the graph — decide in Phase 4.
- **Spotless is a no-op** (applied, unconfigured).
- **No `.editorconfig`**, so ktlint's defaults fight the code's actual style.
- **No CI**: nothing runs the gates automatically.
- **No emulator/AVD installed**, so no verification half can currently be performed.
- `composeOptions.kotlinCompilerExtensionVersion` is still set from the pre-Kotlin-2.x
  era; harmless with the Compose compiler plugin, but dead configuration.
- `androidx.biometric` is pinned to an **alpha** (`1.4.0-alpha05`).
- Gradle reports "deprecated Gradle features were used in this build, making it
  incompatible with Gradle 10" on every run. **Diagnosed**: the single deprecation is
  `ReportingExtension.file(String)`, raised from `build.gradle.kts:16` — that is the
  `apply(plugin = "io.gitlab.arturbosch.detekt")` line, so the caller is the **Detekt
  Gradle plugin**, not project code. Still a warning under Gradle 9.6.0; removing it needs
  a detekt plugin bump, not a build-script edit. Owner: item 4.9.
- **AGP 9.4.1 requires Gradle 9.6.0** (upgraded 2026-09-21 from AGP 9.1.0 / Gradle 9.3.1).
  Kotlin 2.3.10 and KSP 2.3.6 were deliberately left untouched, preserving the KSP ↔ Kotlin
  version lockstep that KSP task resolution depends on. Regression-verified: build, 223
  tests, and both gates behave exactly as before the upgrade.
