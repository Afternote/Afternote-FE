package com.afternote.afternote_fe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 로그인 성공 후 표시되는 임시 화면입니다.
 * 실제 메인 네비게이션 그래프로 교체될 예정입니다.
 */
@Composable
internal fun MainPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "로그인 성공!",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
