package com.afternote.feature.afternote.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/** 맨 앞 원 — 실제 추억 사진이 붙는 유일한 원. */
private val FrontCircleSize: Dp = 96.dp

/** 뒤에 겹치는 장식 원 둘. */
private val EchoCircleSize: Dp = 80.dp

/** 컨테이너 폭. 시안 실측 148 = 뒤 원 왼쪽 68 + 지름 80. */
private val StackWidth: Dp = 148.dp

private val MiddleCircleOffset: Dp = 43.dp

private val BackCircleOffset: Dp = 68.dp

/**
 * 추억 노트 상세 헤더의 겹친 프로필 원 스택 — 정본 시안
 * [node 4327:72823](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72823) 기준 (#463).
 *
 * 실측(컨테이너 148×94, 좌표는 컨테이너 기준):
 *
 * | 원 | 지름 | left | top | 색 | 글리프 |
 * |---|---|---|---|---|---|
 * | 앞 | 96 | 0 | 3 | `#616161` (= `gray7`) | 흰 사람 |
 * | 가운데 | 80 | 43 | 11 | `#BDBDBD` (= `gray4`) | 흰 사람 |
 * | 뒤 | 80 | 68 | 11 | `#E0E0E0` (= `gray3`) | 없음 |
 *
 * 세 원의 중심 y 가 51.5 로 같아 **수직 중앙 정렬**이고, 원 사이 흰 테두리 링은 **없다** — 시안 렌더를
 * 픽셀로 훑어 확인했다(중심 행에서 `#616161` → `#BDBDBD` → `#E0E0E0` 가 경계 픽셀 없이 바로 바뀐다).
 * 앞 원 오른쪽으로 드러나는 폭은 가운데 27, 뒤 25 다.
 *
 * 뒤 두 원은 **장식**이다. 서버 계약
 * [com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia] 의 사진은 `photoUrl`
 * 한 장뿐이라 실데이터가 붙는 원은 맨 앞 하나이고, 나머지 둘은 시안이 그린 깊이 표현이다. 장식이므로
 * 접근성 시맨틱을 주지 않는다 — 스택 전체의 의미는 앞 원([ProfileImage])이 읽어 준다.
 *
 * 색은 디자인 토큰으로 그린다. 앞 원만 core:ui 의 고정 자산(`core_ui_ic_profile_placeholder`, `#616161`
 * 구움)이라 다크 테마에서 토큰과 함께 뒤집히지 않는데, 그 자산은 core:ui 소관이라 이 PR 에서 손대지
 * 않는다.
 */
@Composable
internal fun MemorialProfileStack(
    modifier: Modifier = Modifier,
    profileImageUri: String? = null,
) {
    Box(modifier = modifier.size(width = StackWidth, height = FrontCircleSize)) {
        // 뒤 → 앞 순서로 그린다. 시안 z 순서는 왼쪽(앞) → 오른쪽(뒤)이다.
        EchoCircle(
            color = AfternoteDesign.colors.gray3,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = BackCircleOffset),
        )
        EchoCircle(
            color = AfternoteDesign.colors.gray4,
            showGlyph = true,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = MiddleCircleOffset),
        )
        ProfileImage(
            modifier = Modifier.align(Alignment.CenterStart),
            displayImageUri = profileImageUri,
            size = FrontCircleSize,
        )
    }
}

@Composable
private fun EchoCircle(
    color: Color,
    modifier: Modifier = Modifier,
    showGlyph: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .size(EchoCircleSize)
                .clip(CircleShape)
                .background(color),
    ) {
        if (showGlyph) {
            Image(
                painter = painterResource(R.drawable.afternote_img_memorial_profile_glyph),
                // 장식 원이라 읽어 줄 내용이 없다.
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
