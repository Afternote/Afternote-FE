package com.afternote.core.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * [ContextWrapper] 체인을 따라가 [FragmentActivity]를 찾는다.
 * [androidx.compose.ui.platform.LocalContext]가 래핑된 경우에도
 * 생체 프롬프트 등 Activity를 필요로 하는 곳에서 사용한다.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
