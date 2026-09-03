package com.afternote.feature.setting.presentation.viewmodel

private val KOREAN_MOBILE_PHONE_REGEX = Regex("^01(?:0\\d{8}|[16789]\\d{7,8})$")
private val HYPHENATED_PHONE_REGEX = Regex("^\\d{3}-\\d{3,4}-\\d{4}$")
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun String.normalizeReceiverPhone(): String = filter(Char::isDigit)

internal enum class ReceiverPhoneValidation {
    VALID,
    REQUIRED,
    INVALID,
}

internal fun String.validateReceiverPhone(isRequired: Boolean): ReceiverPhoneValidation {
    if (isBlank()) {
        return if (isRequired) ReceiverPhoneValidation.REQUIRED else ReceiverPhoneValidation.VALID
    }
    val hasValidCharactersAndSeparators = all(Char::isDigit) || matches(HYPHENATED_PHONE_REGEX)
    return if (hasValidCharactersAndSeparators && normalizeReceiverPhone().matches(KOREAN_MOBILE_PHONE_REGEX)) {
        ReceiverPhoneValidation.VALID
    } else {
        ReceiverPhoneValidation.INVALID
    }
}

internal fun String.isValidReceiverEmail(): Boolean = EMAIL_REGEX.matches(trim())
