package com.afternote.core.ui.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/*
* 원형 커스텀 체크박스의 시각·상호작용 상태.
*
* - [Checked]: 짙은 배경([AfternoteDesign.colors.gray9]) + 흰색 체크
* - [CheckedDisabled]: 회색 배경([AfternoteDesign.colors.gray4]) + 흰색 체크, 탭 불가
* - [Unchecked]: 투명 + [AfternoteDesign.colors.gray4] 테두리
*/
enum class CheckboxState {
    Checked,
    CheckedDisabled,
    Unchecked,
}

/**
 * 피그마 기준 원형 체크박스. 아이콘 크기·패딩은 모듈 내부 상수로 고정된다.
 * @param state [CheckboxState]
 */
@Composable
fun AfternoteCircularCheckbox(
    state: CheckboxState,
    modifier: Modifier = Modifier,
) {
    val backgroundColor: Color =
        when (state) {
            CheckboxState.Checked -> AfternoteDesign.colors.gray9
            CheckboxState.CheckedDisabled -> AfternoteDesign.colors.gray4
            CheckboxState.Unchecked -> Color.Transparent
        }

    val borderWidth: Dp =
        if (state == CheckboxState.Unchecked) {
            2.dp
        } else {
            0.dp
        }

    Box(
        modifier =
            modifier
                .size(20.dp)
                .background(color = backgroundColor, shape = CircleShape)
                .then(
                    if (borderWidth > 0.dp) {
                        Modifier.border(
                            width = borderWidth,
                            color = AfternoteDesign.colors.gray4,
                            shape = CircleShape,
                        )
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (state != CheckboxState.Unchecked) {
            Icon(
                painter = painterResource(R.drawable.core_ui_ic_check),
                contentDescription = null,
                tint = AfternoteDesign.colors.white,
                modifier = Modifier.size(width = 10.dp, height = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteCircularCheckboxPreview() {
    AfternoteTheme {
        var checked by remember { mutableStateOf(true) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AfternoteCircularCheckbox(
                state =
                    if (checked) {
                        CheckboxState.Checked
                    } else {
                        CheckboxState.Unchecked
                    },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = CheckboxState.CheckedDisabled,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = CheckboxState.Unchecked,
            )
        }
    }
}
