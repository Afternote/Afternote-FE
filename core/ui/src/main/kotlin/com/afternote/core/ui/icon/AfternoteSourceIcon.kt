package com.afternote.core.ui.icon

import androidx.annotation.DrawableRes
import com.afternote.core.ui.R

/**
 * 애프터노트 **종류** 아이콘의 core 정본 (#926).
 *
 * 수신자 홈이 이 아이콘을 쓰는데, 종전에는 `feature:afternote:presentation` 의 `R` 을 직접
 * 빌려 왔다. 형제 feature 의 리소스를 가로지르는 참조라 두 모듈이 리소스 수준에서 묶인다.
 *
 * 종류는 도메인 개념이지만 **아이콘 선택은 표현**이라, 화면 사이에 오가는 값은 res id 가
 * 아니라 이 enum 이다. ViewModel 이 res id 를 들면 상태가 Android 리소스에 묶여
 * 단위 테스트에서도 리소스가 필요해진다.
 */
enum class AfternoteSourceIcon(
    @param:DrawableRes val drawableResId: Int,
) {
    SocialNetwork(R.drawable.core_ui_afternote_social_pattern),
    GalleryAndFiles(R.drawable.core_ui_afternote_gallery_pattern),
    Memorial(R.drawable.core_ui_afternote_memorial_guideline),

    /** 위 셋으로 갈리지 않는 종류 — 로고로 받는다. */
    Other(R.drawable.core_ui_afternote_logo),
}
