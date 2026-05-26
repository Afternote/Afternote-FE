package com.afternote.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource

/**
 * Presentation 레이어가 ViewModel ↔ Composable 사이에서 "표시할 텍스트" 를 주고받기 위한 타입.
 *
 * ViewModel 은 Context/Resources 에 접근하지 않으므로, 리소스 ID 또는 외부에서 받은 동적 문자열을
 * UiText 로 들고 있다가 Composable 측에서 [asString] 으로 변환한다.
 */
sealed interface UiText {
    data class Resource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Dynamic(
        val value: String,
    ) : UiText

    /** 서버/예외가 메시지를 주면 그대로, 없으면 리소스 fallback. `e.message ?: getString(R.string.x)` 대응. */
    data class DynamicOrResource(
        val value: String?,
        @StringRes val fallbackResId: Int,
    ) : UiText

    companion object {
        fun resource(
            @StringRes resId: Int,
            vararg args: Any,
        ): UiText = Resource(resId, args.toList())
    }
}

@Composable
@ReadOnlyComposable
fun UiText.asString(): String =
    when (this) {
        is UiText.Resource -> {
            if (args.isEmpty()) {
                stringResource(resId)
            } else {
                stringResource(resId, *args.toTypedArray())
            }
        }

        is UiText.Dynamic -> {
            value
        }

        is UiText.DynamicOrResource -> {
            value ?: stringResource(fallbackResId)
        }
    }
