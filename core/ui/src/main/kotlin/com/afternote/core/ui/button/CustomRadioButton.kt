package com.afternote.core.ui.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 커스텀 라디오 버튼 컴포넌트 — **신규 사용 금지, [AfternoteRadioGroup] 을 쓰세요.**
 *
 * 이 컴포저블은 `selected`/`onClick` 만 받는 **단품**이라 라디오의 본질인 단일 선택을 구조로
 * 강제하지 못합니다. 선택값의 소유권이 호출부에 남아, `Set` 토글 상태에 물리면 "라디오
 * 비주얼인데 복수 선택" 이라는 오용이 컴파일 타임에 걸리지 않습니다 (#648 실사고). 접근성도
 * 마찬가지로 호출부 책임이라 `Modifier.selectableGroup` 이 빠지면 스크린리더가 상호배타
 * 관계를 읽지 못합니다.
 *
 * 대신 선택값 하나를 소유하는 [AfternoteRadioGroup] (`selectedValue: T?` + `onSelect(T)`)
 * 을 쓰면 단일 선택·그룹 semantics·48dp 상호작용 경계가 컴포넌트 안에서 닫힙니다.
 *
 * 남아 있는 잔여 사용처는 `SingleSelectionRadioKonsistTest` 의 허용 목록이 정본이며,
 * 이관 완료 시 이 선언을 삭제합니다 (#649).
 *
 * 체크 표시(인디케이터)와 윤곽선 간 간격이 전체 크기의 12분의 1이 되도록 자동 계산됩니다.
 * [onClick]이 있으면 바깥에 [Modifier.minimumInteractiveComponentSize]와
 * [Modifier.selectable] + [Role.RadioButton]을 두어 최소 48×48dp 터치 영역과 리플을 쓰고,
 * 안쪽은 [buttonSize]만큼만 그립니다. 부모가 선택을 처리할 때는 [onClick]을 null로 두면 24dp만 차지합니다.
 *
 * @param selected 선택 여부
 * @param onClick 클릭 이벤트 (null이면 비인터랙티브·부모에서 처리)
 * @param buttonSize 전체 버튼 크기 (기본값: 24.dp)
 * @param selectedColor 선택된 색상 (기본값: AfternoteDesign.colors.gray9)
 * @param unselectedColor 선택 안 된 색상 (기본값: AfternoteDesign.colors.gray4)
 */
@Deprecated(
    message =
        "단품 라디오는 단일 선택을 구조로 강제하지 못한다. " +
            "선택값 하나를 소유하는 AfternoteRadioGroup(options, selectedValue, onSelect) 로 옮겨라 (#649).",
)
@Composable
fun CustomRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    buttonSize: Dp = 24.dp,
    selectedColor: Color = AfternoteDesign.colors.gray9,
    unselectedColor: Color = AfternoteDesign.colors.gray4,
) {
    val borderWidth = 1.dp
    val spacing = buttonSize / 12f
    val maxIndicatorSize = buttonSize - (borderWidth * 2) - (spacing * 2)

    val targetBorderColor = if (selected) selectedColor else unselectedColor
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "CustomRadioButtonBorderColor",
    )

    val indicatorSize by animateDpAsState(
        targetValue = if (selected) maxIndicatorSize else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "CustomRadioButtonIndicatorSize",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .selectable(
                            selected = selected,
                            onClick = onClick,
                            role = Role.RadioButton,
                            interactionSource = interactionSource,
                            indication = ripple(),
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .border(
                        width = borderWidth,
                        color = animatedBorderColor,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (indicatorSize > 0.dp) {
                Box(
                    modifier =
                        Modifier
                            .size(indicatorSize)
                            .background(
                                color = animatedBorderColor,
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}
