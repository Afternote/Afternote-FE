package com.afternote.feature.setting.presentation.shared.component

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
 * 프로필 사진 자리 + 우하단 「추가」 배지. 표시 전용이며 picker 가 붙어 있지 않다.
 *
 * 남은 소비처는 수신인 등록 화면 하나다. 프로필 편집 화면은 사진 선택을 붙이면서(#1438) core:ui 의
 * [ProfileImagePicker][com.afternote.core.ui.ProfileImagePicker] 로 옮겼다. 시안의 프로필 노드가
 * core 자산(`core_ui_ic_profile_placeholder`, 48dp 배지)과 같다는 #2003 의 판정을 따랐다.
 *
 * 이쪽은 `ic_default_profile`(60dp 뷰포트)과 48dp 칸 안에 72dp 로 넘쳐 그려지는 `ic_plus` 를 쓴다.
 * 수신인 등록 화면의 수렴은 #2003 에 남아 있다.
 */
@Composable
fun ProfilePhotoWithAddBadge(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(134.dp)) {
        Image(
            painter = painterResource(R.drawable.setting_ic_default_profile),
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
                painter = painterResource(R.drawable.setting_ic_plus),
                contentDescription = "추가",
                modifier = Modifier.requiredSize(72.dp),
            )
        }
    }
}
