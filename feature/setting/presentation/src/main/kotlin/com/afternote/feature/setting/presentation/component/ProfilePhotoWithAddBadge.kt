package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.feature.setting.presentation.R

/**
 * 프로필 사진 + 우하단 「추가」 배지.
 *
 * 프로필 이미지 크기 134dp 의 단일 출처다(#1782). setting 의 프로필 편집·수신인 등록 두 화면이
 * 같은 블록을 복제하고 있어 여기로 모았다.
 *
 * core:ui 의 [ProfileImagePicker][com.afternote.core.ui.ProfileImagePicker] 로 흡수하지 않은
 * 이유는 자산과 배지 기하가 달라서다 — 이쪽은 `ic_default_profile`(60dp 뷰포트) 과
 * 48dp 칸 안에 72dp 로 넘쳐 그려지는 `ic_plus` 를 쓰고, core 쪽은
 * `core_ui_ic_profile_placeholder` 와 48dp `PlusBadgeButton` 을 쓴다. 바꾸면 픽셀이 움직인다.
 * 수렴은 시안 대조가 선행돼야 하므로 #2003 으로 분리했다.
 *
 * 사진을 띄우는 갈래에서도 대기·실패 표시를 `ic_default_profile` 로 두는 것은 같은 이유다 —
 * core placeholder 로 떨어뜨리면 로딩 한 프레임 동안 다른 자산이 보인다.
 *
 * @param onAddClick 배지를 눌렀을 때. `null` 이면 배지가 눌리지 않는다 — 사진 선택 경로가 없는
 *   표시 전용 화면(수신인 등록)과 저장 중이라 선택을 막아야 하는 순간이 그 자리다. 「눌러도
 *   아무 일 없는 배지」를 그리지 않도록 디폴트를 두지 않는다(`docs/convention/composable-callback-defaults.md`).
 * @param displayImageUri 아바타에 그릴 이미지. 로컬 content URI 든 서버 URL 이든 같다.
 *   비어 있으면 기본 아바타를 그린다.
 */
@Composable
internal fun ProfilePhotoWithAddBadge(
    onAddClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    displayImageUri: String? = null,
) {
    Box(modifier = modifier.size(134.dp)) {
        val defaultProfile = painterResource(R.drawable.ic_default_profile)
        if (displayImageUri.isNullOrBlank()) {
            Image(
                painter = defaultProfile,
                contentDescription = "기본",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = displayImageUri,
                contentDescription = "프로필 사진",
                // 기본 아바타가 뷰포트를 꽉 채운 원이라, 사진도 같은 원으로 잘라야 배지 위치가 그대로 맞는다.
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = defaultProfile,
                error = defaultProfile,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .then(if (onAddClick == null) Modifier else Modifier.clickable(onClick = onAddClick)),
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
