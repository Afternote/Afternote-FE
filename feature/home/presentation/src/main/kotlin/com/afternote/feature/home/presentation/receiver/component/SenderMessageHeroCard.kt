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
            Text(
                text = stringResource(R.string.home_receiver_hero_title, senderName),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray7,
            )
            Text(
                text = date,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
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
