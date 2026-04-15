package com.afternote.feature.afternote.presentation.shared.model

import androidx.annotation.DrawableRes
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

/**
 * 상세 헤더 등에서 쓰는 서비스 표시 묶음.
 *
 * 서비스 이름과 아이콘을 한 몸(Data Coupling)으로 전달해, 호출부가 둘을 따로 계산하다
 * 어긋나는 실수를 방지한다.
 *
 * 서비스 종류는 API 응답 문자열로 식별되고 신규 서비스가 언제든 추가될 수 있으므로
 * enum 대신 data class 로 두었다. 아이콘 해석은 [getIconResForServiceName] 과 동일한
 * 규칙을 따르며, 매핑 밖 문자열은 기본 드로어블로 자연 축소된다.
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
