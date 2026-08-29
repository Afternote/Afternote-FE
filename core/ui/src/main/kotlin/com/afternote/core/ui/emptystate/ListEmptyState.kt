package com.afternote.core.ui.emptystate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 목록 화면의 "등록된 항목이 0건" 빈 상태 — 제목+설명+아이콘+등록 CTA.
 * 수신자 목록(설정)·수신인 목록(타임레터)이 문구·아이콘만 바꿔 이 레이아웃을 공유한다.
 */
@Composable
fun ListEmptyState(
    title: String,
    description: String,
    icon: Painter,
    registerButtonText: String,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(105.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = AfternoteDesign.typography.h1,
                    color = AfternoteDesign.colors.black,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray6,
                )
            }
            Spacer(modifier = Modifier.height(56.dp))
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(134.dp),
            )
        }
        Spacer(modifier = Modifier.height(56.dp))

        AfternoteButton(
            text = registerButtonText,
            onClick = onRegisterClick,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

/** 검색 결과 0건 안내 — 목록 자체는 있으나 필터링 결과가 없을 때. */
@Composable
fun ListSearchEmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
    ) {
        Text(
            text = message,
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}
