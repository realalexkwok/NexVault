package com.nexvault.wallet.domain.model.common

/**
 * Domain-specific exceptions.
 */
open class NexVaultException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class InvalidMnemonicException(
    message: String = "Invalid mnemonic phrase",
) : NexVaultException(message)

class InvalidPrivateKeyException(
    message: String = "Invalid private key",
) : NexVaultException(message)

class InvalidAddressException(
    message: String = "Invalid Ethereum address",
) : NexVaultException(message)

class WalletNotFoundException(
    message: String = "Wallet not found",
) : NexVaultException(message)

class WalletAlreadyExistsException(
    message: String = "Wallet already exists",
) : NexVaultException(message)

class InsufficientBalanceException(
    val available: String,
    val required: String,
) : NexVaultException("Insufficient balance: available=$available, required=$required")

class TransactionFailedException(
    message: String,
    cause: Throwable? = null,
) : NexVaultException(message, cause)

class AuthenticationException(
    message: String = "Authentication failed",
) : NexVaultException(message)

class NetworkException(
    message: String = "Network error",
    cause: Throwable? = null,
) : NexVaultException(message, cause)

/**
 * A network-backed feature needs an API key this build does not have (roadmap 2.0.4).
 *
 * Distinct from [NetworkException] on purpose: the UI shows an explicit "not configured" state
 * naming the missing key instead of a generic network error.
 */
class ApiKeyNotConfiguredException(
    message: String = "API key not configured",
) : NexVaultException(message)

/**
 * The block explorer rejected the call (roadmap 2.0.4b).
 *
 * Carries the explorer's own explanation (for example its "deprecated endpoint" or
 * "invalid API key" text) so the failure is visible instead of a silent empty list.
 */
class ExplorerApiException(
    message: String = "Block explorer error",
) : NexVaultException(message)

/**
 * The configured explorer key cannot reach this chain because the current API plan does not
 * cover it (roadmap 2.0.4b: observed on BNB Smart Chain with the free Etherscan plan).
 *
 * Distinct from [ApiKeyNotConfiguredException] (no key at all) and from [ExplorerApiException]
 * (a genuine rejection): the key works, the plan does not include this chain. A paid plan
 * removes the state without a code change.
 */
class ExplorerPlanUnsupportedException(
    message: String = "This chain is not covered by the current Etherscan plan",
) : NexVaultException(message)

class EncryptionException(
    message: String = "Encryption error",
    cause: Throwable? = null,
) : NexVaultException(message, cause)
