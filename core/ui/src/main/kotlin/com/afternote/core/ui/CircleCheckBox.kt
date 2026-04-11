package com.afternote.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun CircleCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    checkedColor: Color = AfternoteDesign.colors.gray9, // 체크 시 기본 배경색
    uncheckedColor: Color = Color.Transparent, // 해제 시 기본 배경색
    checkIconColor: Color = AfternoteDesign.colors.white,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 1. 배경색과 테두리 색상의 부드러운 애니메이션 처리
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) checkedColor else uncheckedColor,
        animationSpec = tween(150),
        label = "checkbox_bg_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else AfternoteDesign.colors.gray4,
        animationSpec = tween(150),
        label = "checkbox_border_color",
    )

// 2. Surface를 제거하고 Modifier 체이닝으로 렌더링 최적화
    Box(
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = 20.dp,
                    minHeight = 20.dp,
                ) // 🔥 수정: 내부 아이콘이 사라져도 크기가 0으로 붕괴되지 않음
                .then(
                    if (onCheckedChange != null) {
                        Modifier.toggleable(
                            value = checked,
                            onValueChange = onCheckedChange,
                            role = Role.Checkbox,
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false),
                        )
                    } else {
                        Modifier.semantics { role = Role.Checkbox }
                    },
                ).clip(CircleShape)
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(animationSpec = tween(150)),
            exit = scaleOut(animationSpec = tween(150)),
        ) {
            Icon(
                painter = painterResource(R.drawable.core_ui_ic_check),
                contentDescription = null, // 부모 Row에서 설명하므로 null 유지
                tint = checkIconColor,
                modifier = Modifier.width(12.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleCheckBoxPreview() {
    AfternoteTheme {
        Column {
            // 외부에서 번거롭게 Color를 분기처리 할 필요가 없어졌습니다.
            CircleCheckBox(
                checked = true,
                onCheckedChange = {},
                modifier = Modifier.size(20.dp),
            )
            CircleCheckBox(
                checked = true,
                onCheckedChange = {},
                checkedColor = AfternoteDesign.colors.gray4, // 특정 화면에서 색이 다를 경우만 오버라이드
                modifier = Modifier.size(20.dp),
            )
            CircleCheckBox(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
