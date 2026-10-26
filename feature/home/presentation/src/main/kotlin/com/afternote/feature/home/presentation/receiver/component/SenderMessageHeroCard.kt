package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.home.presentation.R

/**
 * 고인이 남긴 한 마디 + 작성일 + 본문을 보여주는 그라데이션 카드.
 */
@Composable
fun SenderMessageHeroCard(
    senderName: String,
    date: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(brush = heroGradient())
                .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 제목에만 weight 를 준다 — Row 는 가중치 없는 자식을 **먼저** 재므로, 작성일이 자기
            // 폭을 온전히 받고 제목이 남은 폭을 쓴다. 둘 다 가중치가 없던 종전에는 긴 제목이
            // 가용 폭을 다 써 뒤에 재는 작성일이 0dp 를 받았고, 큰 글씨(배율 2.0)와 8글자 이름이
            // 겹치면 「2026.09.13」 이 열 줄로 쪼개졌다 (#2032).
            //
            // fill = false 라 제목은 제 폭만 차지한다 — 짧은 이름에서는 SpaceBetween 이 종전과
            // 같은 자리에 둘을 놓는다. end padding 은 제목과 작성일이 맞붙지 않게 하는 최소 간격이고,
            // 제목이 그 폭을 다 쓰지 않는 경우에는 보이는 위치를 바꾸지 않는다.
            Text(
                text = stringResource(R.string.home_receiver_hero_title, senderName),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray7,
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
            )
            // 날짜는 형식이 고정돼 있어 줄을 나눌 이유가 없다 — 한 줄로 못 담을 만큼 좁아지면
            // 쪼개는 대신 잘라서, 한 글자씩 세로로 서는 모양이 어떤 배율에서도 다시 나오지 않게 한다.
            Text(
                text = date,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

/**
 * SenderMessageHeroCard 전용 파스텔 그라데이션 (그린 → 크림 → 피치).
 *
 * 본 화면 1회용이고 다른 화면의 그라데이션과 색 조합이 달라, 디자인 시스템 토큰으로 추상화하면
 * 재사용성보다 SSOT 위반 비용이 큼.
 * 공용 색상 토큰으로 일반화하기 어려운 화면 전용 예외라 이 한 곳에 고정.
 */
private fun heroGradient(): Brush =
    Brush.linearGradient(
        colors =
            listOf(
                Color(0xFFD7E8DA),
                Color(0xFFEFF1E8),
                Color(0xFFF8E9DA),
            ),
    )
