package com.afternote.feature.afternote.presentation.shared.model

import androidx.annotation.DrawableRes
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

/**
 * 상세 헤더 등에서 쓰는 서비스 표시 묶음.
 *
 * 서비스 이름과 아이콘을 한 몸(Data Coupling)으로 전달해, 호출부가 둘을 따로 계산하다
 * 어긋나는 실수를 방지한다.
 *
 * API 제목 문자열은 [AfternoteService] 카탈로그에 없을 수 있으므로 이 타입으로 감싼다.
 * 아이콘 해석은 [getIconResForServiceName] 과 동일한 규칙(알려진 표시명 → enum, 그 외 → 카테고리 기본 아이콘)을 따른다.
 */
data class AfternoteServiceDisplay(
    val serviceName: String,
    @DrawableRes val iconResId: Int,
) {
    companion object {
        /** API 서비스 이름으로부터 아이콘을 해석해 묶음을 만든다. */
        fun fromServiceName(serviceName: String): AfternoteServiceDisplay =
            AfternoteServiceDisplay(
                serviceName = serviceName,
                iconResId = getIconResForServiceName(serviceName),
            )
    }
}
