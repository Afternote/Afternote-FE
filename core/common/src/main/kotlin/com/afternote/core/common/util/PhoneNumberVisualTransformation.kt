package com.afternote.core.common.util

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.insert

private const val PHONE_MAX_DIGITS = 11

/**
 * 한국 전화번호 입력용: 숫자만 허용, 최대 [PHONE_MAX_DIGITS]자리.
 *
 * [PhoneNumberVisualTransformation]과 함께 쓰는 것을 권장합니다.
 */
val PhoneNumberInputTransformation: InputTransformation =
    InputTransformation {
        val seq = asCharSequence()
        if (!seq.all { it.isDigit() } || seq.length > PHONE_MAX_DIGITS) {
            revertAllChanges()
        }
    }

/**
 * 한국 전화번호 표시 포맷 (`010-XXX-XXXX` 또는 `010-XXXX-XXXX`)용 [androidx.compose.foundation.text.input.OutputTransformation].
 *
 * 상태에는 하이픈 없이 숫자만 두고, 화면에만 하이픈을 넣습니다.
 * [PhoneNumberInputTransformation] 등으로 숫자만 들어오게 맞추는 것을 권장합니다.
 */
val PhoneNumberVisualTransformation: OutputTransformation =
    OutputTransformation {
        val originalLength = length
        if (originalLength <= 3) return@OutputTransformation

        if (originalLength <= 7) {
            insert(3, "-")
        } else if (originalLength == 10) {
            insert(3, "-")
            insert(7, "-")
        } else if (originalLength >= 11) {
            insert(3, "-")
            insert(8, "-")
        } else {
            insert(3, "-")
        }
    }
