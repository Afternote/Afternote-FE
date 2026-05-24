package com.afternote.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Compose 환경 등에서 현재 [Context]로부터 원하는 타입의 [Activity]를 안전하게 추출합니다.
 * [ContextWrapper] 체인을 따라가며 찾고, 없으면 null을 반환합니다.
 *
 * 사용 예:
 * ```
 * val activity = context.findActivity<FragmentActivity>()
 * val baseActivity = context.findActivity<Activity>()
 * ```
 */
inline fun <reified T : Activity> Context.findActivity(): T? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is T) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? T
}
