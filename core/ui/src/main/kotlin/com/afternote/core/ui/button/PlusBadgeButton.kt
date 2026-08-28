package com.afternote.core.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 다른 UI 요소(프로필 사진 등)에 붙어있는 뱃지형/인라인 원형 버튼.
 *
 * [FloatingActionButton][androidx.compose.material3.FloatingActionButton]과 달리
 * 그림자·최소 레이아웃 크기를 강제하지 않아 시각 크기는 작게 유지합니다. [onClick]이 있으면
 * Foundation hit testing이 실제 터치 영역을 48dp까지 자동 확장합니다.
 *
 * [onClick]이 `null`이면 상위 컨테이너가 단일 클릭 영역을 소유할 때 쓰는 장식 배지다.
 * 이때 [contentDescription]도 `null`로 두어 같은 액션을 중복해 읽지 않게 한다.
 */
@Composable
fun PlusBadgeButton(
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(8.dp),
    size: Dp = 30.dp,
) {
    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(AfternoteDesign.colors.black)
            .then(clickModifier)
            .padding(paddingValues),
    ) {
        Icon(
            painter = painterResource(R.drawable.core_ui_circle_button_plus),
            contentDescription = contentDescription,
            tint = AfternoteDesign.colors.white,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlusBadgeButtonPreview() {
    AfternoteTheme {
        PlusBadgeButton(
            contentDescription = "Plus Button",
            onClick = {},
            paddingValues = PaddingValues(8.dp),
        )
    }
}
