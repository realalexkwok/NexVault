# 2.0.6 — TC traceability matrix

> The authoritative register. `doc/07-TEST-CASES.md` stays frozen as the definition source; this
> file maps each of its 52 ids to exactly one test method (marker `// TC-XXX-NNN` directly above
> the `@Test`) or registers it as unimplemented with a named owning roadmap item.
>
> Status legend: **covered** (mapped method exists and passes) · **registered** (no test method can
> exist today — owning roadmap item named) · a `2.0.6` note marks rows this item added/changed.
>
> Audit: every id below was extracted from `doc/07`; the marker set was extracted from the test
> sources; the two sets plus the registered set must be equal — see `validation.md` §A6.

## Mapped (25 of 52)

| TC id | Mapped test method (`<module>/src/test/...`) | Notes |
| --- | --- | --- |
| TC-SEC-001 | `core-security/.../mnemonic/MnemonicManagerTest.testGenerate12WordMnemonic` | 2.0.6 strengthened: now also re-validates the checksum. |
| TC-SEC-002 | `MnemonicManagerTest.testValidMnemonicPassesValidation` | |
| TC-SEC-003 | `MnemonicManagerTest.testInvalidMnemonicWrongChecksum` | 2.0.6: **new** — 12 valid words, wrong checksum ("zoo" last). The old same-named test was actually a word-count case; renamed (see SEC-004). |
| TC-SEC-004 | `MnemonicManagerTest.testInvalidMnemonicWrongWordCount` | 2.0.6: renamed from the misleading `testInvalidMnemonicWrongChecksum`; asserts 11 words → false (the doc/07 "10-word" intent). |
| TC-SEC-005 | `core-security/.../wallet/HDKeyManagerTest.testDeriveAddressFromKnownMnemonic` | Canonical vector → `0x9858EfFD…` |
| TC-SEC-006 | `core-security/.../encryption/EncryptionManagerTest.testEncryptDecryptWithPassword` | Password path. The AndroidKeyStore half is `@Ignore`d → registered to **4.13**. |
| TC-SEC-007 | `core-security/.../util/SecurityUtilsTest.testPasswordHashVerifiesForCorrectPassword` | 2.0.6: **new**. doc/07's `WalletSecureStorage.storePin` API is stale — the real chain is `AuthRepositoryImpl` → `SecurityUtils.hashPassword/verifyPassword`; behavioural twin `AuthRepositoryImplTest.verifyPin_correctPin_returnsSuccess`. |
| TC-SEC-008 | `SecurityUtilsTest.testPasswordHashRejectsIncorrectPassword` | 2.0.6: **new**; twin `AuthRepositoryImplTest.verifyPin_incorrectPin_returnsFailed`. |
| TC-UC-001 | `domain/.../usecase/wallet/CreateWalletUseCaseTest.createWallet_success_returnsWalletWithNonEmptyAddress` | 2.0.6: **new**. doc/07's no-arg `CreateWalletUseCase()` → `Result<Wallet>` is stale; real contract `(walletName, draft)` → `DataResult<WalletCreationResult>`; intent (non-empty address) asserted. |
| TC-UC-002 | `domain/.../usecase/ImportFromMnemonicUseCaseTest.testWithValid12WordsCallsRepository` | |
| TC-UC-003 | `ImportFromMnemonicUseCaseTest.testWith10WordsReturnsError` | Asserts `InvalidMnemonicException`. |
| TC-UC-004 | `domain/.../usecase/ImportFromPrivateKeyUseCaseTest.testWithValid64HexCallsRepository` | |
| TC-NET-001 | `core-network/.../adapter/BigDecimalAdapterTest.stringToken_parsesExactlyWithoutPrecisionLoss` | 2.0.6: **new**. String token parses exactly; the number-token double route is proven lossy by a companion test. |
| TC-NET-002 | `core-network/.../util/AddressValidatorTest.fortyTwoCharHexAddressWith0xPrefix_isAccepted` | 2.0.6: **new**. doc/07's `AddressAdapter` throwing `JsonDataException` is stale — the shipped API is `AddressValidator.isValid(): Boolean`; the accept/reject intent is asserted against it. |
| TC-NET-003 | `AddressValidatorTest.addressWithout0xPrefix_isRejected` | 2.0.6: **new**; same API note. |
| TC-DB-001 | `core-database/.../dao/NexVaultDaoTest.upsertingTheSameToken_updatesTheBalanceInsteadOfDuplicating` | 2.0.6: **new**, Robolectric + in-memory Room. |
| TC-DB-002 | `NexVaultDaoTest.getTokensByChain_returnsOnlyThatChainOrderedByFiatValueDescending` | 2.0.6: **new**; asserts via `getTokensByChainOnce`, the one-shot twin of the same query. |
| TC-DB-003 | `NexVaultDaoTest.getTransactions_returnsTheTenNewestForTheRequestedPage` | 2.0.6: **new**; 25 rows, page 1 = 10 newest. |
| TC-VM-001 | `feature-home/.../HomeViewModelTest.initialLoad_transitionsFromLoadingToSuccessWithThreeTokensAndTotalValue` | 2.0.6: **new**; asserts the Loading → Success transition itself. |
| TC-VM-002 | `HomeViewModelTest.refreshError_showsNetworkMessageAndKeepsCachedData` | 2.0.6: **new**. doc/07 blames `GetPortfolioUseCase` throwing; the real pull-to-refresh path is `RefreshBalancesUseCase` failing with `IOException` → `home_error_network` ("Network error. Please check your connection.") with cached data kept — intent covered on the real path. |
| TC-VM-006 | `feature-onboarding/.../CreateWalletViewModelTest.draftLoadedOnInitWithoutPersisting` | 2.0.6 strengthened: 12 words **non-empty**. |
| TC-VM-007 | `feature-onboarding/.../VerifyMnemonicViewModelTest.correctOrder` | `isVerified` true; confirm-enablement is the screen's half. |
| TC-VM-008 | `VerifyMnemonicViewModelTest.incorrectOrder` | 2.0.6 strengthened: now also asserts the post-error reset to the shuffled words. |
| TC-VM-009 | `feature-auth/.../UnlockViewModelTest.TC-UNLOCK-015 fifth failure locks out for thirty seconds` | 2.0.6: **new**; alias of the local `TC-UNLOCK-*` scheme (below); asserts `isLockedOut`, 30 s countdown, and the post-lockout reset. |
| TC-INT-001 | `app/src/androidTest/.../CreationFlowE2ETest` | 2.0.6: **new** instrumented E2E (create → verify → PIN → Home); see `validation.md` §A5. |

## Registered (27 of 52)

| TC id | Owning roadmap item | Why there is no test home today |
| --- | --- | --- |
| TC-REPO-001 | **4.8** | Deep form needs a mocked Web3j balance response; `Web3j` is final and unmockable with the current stack. Partial today: `TokenRepositoryImplTest.refreshBalances_withRpcKey_reachesTheNetwork` (reach-only), `RefreshBalancesUseCaseTest`, and the 2.0.5 device walk (live balances rendered). |
| TC-REPO-002 | **4.8** | Same constraint for the ERC-20 metadata happy path; partial: key-gate test + `AddCustomTokenUseCaseTest`. |
| TC-REPO-003 | **2.6** | `sendNativeTransaction` throws `UnsupportedOperationException`. |
| TC-REPO-004 | **2.6** | Same — send flow is 2.6. |
| TC-REPO-005 | **2.8** | **Mapped (2026-10-02)** — `page`/`pageSize` are honoured end-to-end (DAO limit/offset) and the explorer refresh forwards its page; `TransactionHistoryPagingTest` pins page 3 → offset 40, the page forwarded to the explorer and the absurd-page clamp. |
| TC-VM-003 | **2.6** | No `SendViewModel` (feature-send is an empty module). |
| TC-VM-004 | **2.6** | Same. |
| TC-VM-005 | **2.6** | Same (MAX button + gas math arrive with 2.6). |
| TC-VM-010 | **3.3** | No `SwapViewModel` (feature-swap is an empty module). |
| TC-UI-001 | **4.18** | WelcomeScreen exists; no instrumented UI tests anywhere (2.0.2b handoff replanned here). |
| TC-UI-002 | **4.18** | CreateWalletScreen exists. |
| TC-UI-003 | **4.18** | `PinInputField` exists in core-ui (finding 1.2-5 residue). |
| TC-UI-004 | **4.18** | HomeScreen exists. |
| TC-UI-005 | **4.18** | PullToRefreshBox exists on HomeScreen. |
| TC-UI-006 | **2.7** | **Mapped (2026-09-30)** to `SendFlowE2ETest#sendForm_qrButtonNavigatesToTheScanner` — the send form's QR icon opens `QrScannerScreen` and back returns to the form, green on the Pixel 6a (11/11). |
| TC-UI-007 | **3.2** | NFT gallery screen does not exist. |
| TC-UI-008 | **2.8** | **Mapped (2026-10-02)** — `HistoryScreen` exists with sticky date groups, the AC's five chips, fetch-on-scroll and the detail route; `HistoryScreenTest` + `HistoryViewModelTest` pin the behaviour. |
| TC-UI-009 | **3.4** | The Settings tab is a `PlaceholderTabScreen`. |
| TC-UI-010 | **2.6** | No `ConfirmationDialog` component exists; the send review step will build it. |
| TC-SECTEST-001 | **4.7** | Logcat scan around the creation flow — pairs with the 4.7 security pass. |
| TC-SECTEST-002 | **4.7** | Private-key zeroing after signing — signing arrives in 2.6; 4.7 owns the hardening. |
| TC-SECTEST-003 | **4.7** | `FLAG_SECURE` on sensitive screens is 4.7's stated scope. |
| TC-SECTEST-004 | **4.13** | Device file-inspection test alongside the AndroidKeyStore test porting. |
| TC-SECTEST-005 | **3.4** | The auto-lock setting lives in Settings (3.4). |
| TC-INT-002 | **2.6** | **Mapped (2026-09-30)** to `SendFlowE2ETest` — the form walk (PIN unlock, Sepolia switch, on-device validations, disabled submit) is green on the Pixel 6a; the **funded on-chain submit** (hash + explorer) is deferred to the **2.10** Phase 2 walk — the faucet is captcha-walled (owner decision, 2.6 validation.md Task 0f). |
| TC-INT-003 | **3.1** | WalletConnect pairing is 3.1. |
| TC-INT-004 | **3.4** | Needs the Settings export + delete-wallet UI (3.4). |

## Aliases — the pre-existing `TC-UNLOCK-*` scheme

`feature-auth` numbers its 15 unlock tests locally. Kept as-is (no renames); the matrix above maps
doc/07 ids over them:

- `TC-UNLOCK-015` = **TC-VM-009** (mapped, above).
- `TC-UNLOCK-001…014` cover PIN-entry and biometric behaviours doc/07 does not enumerate — they are
  **supporting coverage, not a TC id**, and carry no `// TC-` marker.

## Beyond doc/07 (2.0.6 additions without a TC id)

| Test | Why |
| --- | --- |
| `feature-tokens/.../TokenDetailViewModelTest` (6 tests) | Closes legacy finding 2.5-1; doc/07 has no token-detail TC. Includes the two notice cases that were dead code before the `RefreshTransactionHistoryUseCase` fix. |
| `domain/.../usecase/RefreshTransactionHistoryUseCaseTest` (3 tests) | Regression lock for that fix (success passthrough, error passthrough, no-wallet guard). |
| Supporting assertions added to `BigDecimalAdapterTest` / `AddressValidatorTest` / `NexVaultDaoTest` | Companion cases around each mapped method. |
