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
| Build system | Gradle 9.3.1 (wrapper) + Kotlin DSL + version catalog | `gradle-wrapper.properties` |
| Android Gradle Plugin | 9.1.0 | `libs.versions.toml` |
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

### Jetpack / AndroidX

| Library | Version | Use |
| --- | --- | --- |
| Compose BOM | 2026.03.00 | UI toolkit (Material 3) |
| `androidx.compose.material3` + `material-icons-extended` | via BOM | Components, icons |
| `androidx.navigation:navigation-compose` | 2.9.7 | Navigation graphs |
| `androidx.hilt:hilt-navigation-compose` | 1.3.0 | `hiltViewModel()` |
| `androidx.lifecycle:*` | 2.10.0 | ViewModel, `collectAsStateWithLifecycle` |
| Hilt | 2.59.2 | DI |
| Room | 2.8.4 | Local database (`core-database`, schema JSON in `core/core-database/schemas/`) |
| DataStore Preferences | 1.2.1 | Settings, wallet metadata, secure app state |
| WorkManager | 2.11.1 | Background sync (Phase 3, not yet used) |
| CameraX | 1.5.3 | QR scanning |
| Biometric | 1.4.0-alpha05 | Unlock (note: alpha) |
| `androidx.core:core-ktx` | 1.18.0 | Core extensions |

### Network & blockchain

| Library | Version | Use |
| --- | --- | --- |
| Retrofit | 3.0.0 | REST clients (CoinGecko, Etherscan-compatible explorers) |
| OkHttp + logging-interceptor | 5.3.2 | HTTP, cache-control interceptor |
| Moshi (`moshi-kotlin-codegen` via KSP) | 1.15.2 | JSON |
| kotlinx-serialization-json | 1.10.0 | Backup serialization |
| Web3j (`org.web3j:core`) | 5.0.2 | JSON-RPC, ABI, transaction signing |
| WalletConnect Android BOM | 1.35.2 | DApp sessions (Phase 3) |
| kethereum `bip39` | 0.86.0 | BIP-39 / BIP-32 / BIP-44 |

API surface in `core:core-network`: `CoinGeckoApi`, `BlockExplorerApi` +
`BlockExplorerApiFactory`, `Web3jProvider`, `ChainConfigProvider`/`ChainNetworkConfig`,
`AddressValidator`, `BigDecimalAdapter`/`BigIntegerAdapter`, `CacheControlInterceptor`,
`CoinGeckoApiKeyInterceptor`.

### Security

| Library / API | Version | Use |
| --- | --- | --- |
| AndroidKeyStore | platform | Hardware-backed key wrapping |
| Google Tink (`tink-android`) | 1.20.0 | AEAD primitives over KeyStore |
| kethereum bip39 | 0.86.0 | Mnemonic generation/validation |
| SQLCipher | — | **Not adopted.** Room is unencrypted today; deferred to Phase 4 |

Implemented in `core:core-security`: `EncryptionManager`, `KeyStoreManager`,
`MnemonicManager`, `HDKeyManager`, `WalletStore`, `BiometricHelper`,
`PasswordValidator`, `SecurityUtils`.

### UI, media, tooling

| Library | Version | Use |
| --- | --- | --- |
| Coil Compose | 2.7.0 | Image loading (NFT thumbnails, token icons) |
| Lottie Compose | 6.7.1 | Animated illustrations |
| Vico Compose | 3.0.3 | Portfolio charts |
| ML Kit barcode-scanning | 17.3.0 | QR scanning |
| ZXing | 3.5.3 | QR generation |
| Accompanist permissions / systemuicontroller | 0.37.3 | Permissions, system bars |
| Shimmer | 1.2.0 | Loading placeholders |
| LeakCanary (`debugImplementation`) | 2.14 | Leak detection |

## 5. Testing

| Library | Version | Status |
| --- | --- | --- |
| JUnit 4 | 4.13.2 | **The actual standard** — every module uses `testImplementation(libs.junit)` |
| MockK | 1.14.9 | **The mocking library.** Mockito is not a dependency anywhere |
| kotlinx-coroutines-test | 1.10.2 | `runTest`, `advanceUntilIdle` |
| Turbine | 1.2.1 | Flow assertions |
| Truth | 1.4.5 | Fluent assertions |
| Robolectric | 4.16.1 | JVM Android tests (declared, not yet used) |
| `androidx.compose.ui:ui-test-*` | 1.10.5 | Compose UI tests |
| `hilt-android-testing` | 2.59.2 | DI-aware tests |
| JUnit 5 (`junit-jupiter` 6.0.3) | — | **Catalog-only, unused.** Either adopt or delete — open decision in Phase 4 |

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
| Detekt | 1.23.8 | `config/detekt/detekt.yml` (`build.maxIssues: 0`, `maxLineLength: 120`, `TooManyFunctions thresholdInFiles: 11`, `weights.complexity: 2`) | **75 issues → fail** |
| ktlint | 14.2.0 (plugin), ktlint 1.x defaults | no `.editorconfig` | **1656 violations across 178 of 221 Kotlin files → fail** |
| Spotless | 8.3.0 | **applied with no configuration block** | no-op — never cite it as evidence |

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
  incompatible with Gradle 10" on every run; the build is not yet Gradle 10 ready
  (re-run with `--warning-mode all` to enumerate them).
