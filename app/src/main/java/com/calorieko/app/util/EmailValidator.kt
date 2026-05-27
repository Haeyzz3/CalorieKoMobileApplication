package com.calorieko.app.util

enum class EmailValidationIssue {
    NONE,
    BLANK,
    INVALID_FORMAT,
    DOMAIN_TYPO
}

data class EmailValidationResult(
    val normalizedEmail: String,
    val isValid: Boolean,
    val message: String?,
    val suggestedEmail: String?,
    val issue: EmailValidationIssue
)

object EmailValidator {
    private val knownDomainTypos = mapOf(
        "gmail.co" to "gmail.com",
        "gmail.con" to "gmail.com",
        "gmail.cmo" to "gmail.com",
        "gmai.com" to "gmail.com",
        "gmial.com" to "gmail.com",
        "gnail.com" to "gmail.com",
        "hotmial.com" to "hotmail.com",
        "hotmai.com" to "hotmail.com",
        "hotmail.co" to "hotmail.com",
        "outlok.com" to "outlook.com",
        "outllok.com" to "outlook.com",
        "outlook.co" to "outlook.com",
        "yaho.com" to "yahoo.com",
        "yahoo.co" to "yahoo.com",
        "yahoo.con" to "yahoo.com",
        "icloud.co" to "icloud.com",
        "protonmail.co" to "protonmail.com"
    )

    fun validate(input: String): EmailValidationResult {
        val normalizedEmail = normalize(input)
        if (normalizedEmail.isBlank()) {
            return invalid(
                normalizedEmail = normalizedEmail,
                message = "Please enter your email address.",
                issue = EmailValidationIssue.BLANK
            )
        }

        val atIndex = normalizedEmail.indexOf('@')
        if (atIndex <= 0 || atIndex != normalizedEmail.lastIndexOf('@') || atIndex == normalizedEmail.lastIndex) {
            return invalidFormat(normalizedEmail)
        }

        val localPart = normalizedEmail.substring(0, atIndex)
        val domain = normalizedEmail.substring(atIndex + 1)

        if (!isValidLocalPart(localPart) || !isValidDomain(domain)) {
            return invalidFormat(normalizedEmail)
        }

        findSuggestedDomain(domain)?.let { suggestedDomain ->
            val suggestedEmail = "$localPart@$suggestedDomain"
            return EmailValidationResult(
                normalizedEmail = normalizedEmail,
                isValid = false,
                message = "Did you mean $suggestedEmail?",
                suggestedEmail = suggestedEmail,
                issue = EmailValidationIssue.DOMAIN_TYPO
            )
        }

        return EmailValidationResult(
            normalizedEmail = normalizedEmail,
            isValid = true,
            message = null,
            suggestedEmail = null,
            issue = EmailValidationIssue.NONE
        )
    }

    fun normalize(input: String): String {
        val trimmed = input.trim()
        val atIndex = trimmed.lastIndexOf('@')
        if (atIndex <= 0 || atIndex == trimmed.lastIndex) return trimmed

        val localPart = trimmed.substring(0, atIndex)
        val domain = trimmed.substring(atIndex + 1).lowercase()
        return "$localPart@$domain"
    }

    private fun invalidFormat(normalizedEmail: String): EmailValidationResult {
        return invalid(
            normalizedEmail = normalizedEmail,
            message = "Please enter a valid email address.",
            issue = EmailValidationIssue.INVALID_FORMAT
        )
    }

    private fun invalid(
        normalizedEmail: String,
        message: String,
        issue: EmailValidationIssue
    ): EmailValidationResult {
        return EmailValidationResult(
            normalizedEmail = normalizedEmail,
            isValid = false,
            message = message,
            suggestedEmail = null,
            issue = issue
        )
    }

    private fun isValidLocalPart(localPart: String): Boolean {
        if (localPart.isBlank() || localPart.length > 64) return false
        if (localPart.startsWith(".") || localPart.endsWith(".")) return false
        if (".." in localPart) return false
        return localPart.all { char ->
            char.isAsciiLetterOrDigit() || char in localPartSymbols
        }
    }

    private fun isValidDomain(domain: String): Boolean {
        if (domain.isBlank() || domain.length > 253) return false
        if (domain.startsWith(".") || domain.endsWith(".")) return false
        if (".." in domain) return false

        val labels = domain.split(".")
        if (labels.size < 2) return false

        val topLevelDomain = labels.last()
        if (topLevelDomain.length < 2 || !topLevelDomain.all { it in 'a'..'z' }) return false

        return labels.all { label ->
            label.isNotBlank() &&
                label.length <= 63 &&
                !label.startsWith("-") &&
                !label.endsWith("-") &&
                label.all { it.isAsciiLetterOrDigit() || it == '-' }
        }
    }

    private fun findSuggestedDomain(domain: String): String? {
        return knownDomainTypos[domain]
    }

    private fun Char.isAsciiLetterOrDigit(): Boolean {
        return this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'
    }

    private val localPartSymbols = setOf('.', '_', '%', '+', '-')
}
