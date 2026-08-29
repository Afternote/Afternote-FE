package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 회원가입 입력 필드 상단 라벨.
 *
 * 단순 텍스트만 노출하며 디자인 시스템의 bodyBase 스타일과 gray9 색상을 사용합니다.
 */
@Composable
fun SignUpInputLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = AfternoteDesign.typography.bodyBase,
        color = AfternoteDesign.colors.gray9,
    )
}
