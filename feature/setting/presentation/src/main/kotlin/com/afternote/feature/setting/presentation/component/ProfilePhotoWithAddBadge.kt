package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.feature.setting.presentation.R

/**
 * 프로필 사진 자리 + 우하단 「추가」 배지. 표시 전용이며 아직 picker 가 붙어 있지 않다.
 *
 * 프로필 이미지 크기 134dp 의 단일 출처다(#1782). setting 의 프로필 편집·수신인 등록 두 화면이
 * 같은 블록을 복제하고 있어 여기로 모았다.
 *
 * core:ui 의 [ProfileImagePicker][com.afternote.core.ui.ProfileImagePicker] 로 흡수하지 않은
 * 이유는 자산과 배지 기하가 달라서다 — 이쪽은 `ic_default_profile`(60dp 뷰포트) 과
 * 48dp 칸 안에 72dp 로 넘쳐 그려지는 `ic_plus` 를 쓰고, core 쪽은
 * `core_ui_ic_profile_placeholder` 와 48dp `PlusBadgeButton` 을 쓴다. 바꾸면 픽셀이 움직인다.
 * 수렴은 시안 대조가 선행돼야 하므로 #2003 으로 분리했다.
 */
@Composable
fun ProfilePhotoWithAddBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(134.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_default_profile),
            contentDescription = "기본",
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = "추가",
                modifier = Modifier.requiredSize(72.dp),
            )
        }
    }
}
