package com.nexvault.wallet.core.security.validation

object PasswordValidator {
    private const val MIN_PASSWORD_LENGTH = 8
    private const val PIN_LENGTH = 6

    fun validate(password: String): PasswordValidationResult {
        val hasMinLength = password.length >= MIN_PASSWORD_LENGTH
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val isValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar

        return PasswordValidationResult(
            isValid = isValid,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar,
        )
    }

    fun calculateStrength(password: String): Int {
        if (password.isEmpty()) return 0

        var score = 0

        // Length contribution (max 25 points)
        score += when {
            password.length >= 16 -> 25
            password.length >= 12 -> 20
            password.length >= 10 -> 15
            password.length >= 8 -> 10
            else -> 5
        }

        // Character variety contribution (max 50 points)
        if (password.any { it.isLowerCase() }) score += 10
        if (password.any { it.isUpperCase() }) score += 10
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 15

        // Pattern penalties (max 25 points deduction)
        // Consecutive characters
        var consecutiveCount = 0
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                consecutiveCount++
            }
        }
        score -= (consecutiveCount * 5).coerceAtMost(15)

        // Common patterns
        val commonPatterns = listOf("123", "abc", "qwerty", "password", "admin")
        for (pattern in commonPatterns) {
            if (password.lowercase().contains(pattern)) {
                score -= 10
            }
        }

        return score.coerceIn(0, 100)
    }

    fun validatePin(pin: String): Boolean {
        if (pin.length != PIN_LENGTH) return false
        return pin.all { it.isDigit() }
    }
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
)
