package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService

/** Label and icon drawable resource IDs for an afternote type key. */
data class AfternoteRes(
    val stringResId: Int,
    val drawableResId: Int,
)

/**
 * @param typeKey Writer: e.g. SOCIAL_NETWORK, GALLERY_AND_FILES, MEMORIAL. Receiver: e.g. INSTAGRAM, GALLERY, GUIDE, NAVER_MAIL.
 */
fun getAfternoteDisplayRes(typeKey: String): AfternoteRes {
    AfternoteService.fromTypeKeyOrNull(typeKey)?.let { svc ->
        return AfternoteRes(stringResId = svc.stringResId, drawableResId = svc.iconResId)
    }
    val drawableResId =
        AfternoteService.fromDisplayKeyOrNull(typeKey)?.iconResId
            ?: R.drawable.feature_afternote_img_logo
    return AfternoteRes(stringResId = R.string.afternote_category_social_network, drawableResId = drawableResId)
}

/**
 * Icon drawable res for an [AfternoteServiceType]. Same mapping as [getAfternoteDisplayRes]; use when you have [AfternoteServiceType].
 */
fun getIconResForServiceType(serviceType: AfternoteServiceType): Int =
    AfternoteService.fromTypeKeyOrNull(serviceType.name)?.iconResId
        ?: R.drawable.feature_afternote_img_logo

/**
 * Uses the same display-key resolution as receiver typeKey lookups.
 */
fun getIconResForServiceName(serviceName: String): Int =
    AfternoteService.fromDisplayKeyOrNull(serviceName)?.iconResId
        ?: getIconResForServiceType(AfternoteServiceCatalog.serviceTypeFor(serviceName))

/**
 * API typeKey(예: "INSTAGRAM") → 화면 표시명(예: "인스타그램") 변환.
 * 매핑이 없으면 typeKey를 그대로 반환합니다.
 */
fun getServiceNameForTypeKey(typeKey: String): String = AfternoteService.fromTypeKeyOrNull(typeKey)?.displayKey ?: typeKey
