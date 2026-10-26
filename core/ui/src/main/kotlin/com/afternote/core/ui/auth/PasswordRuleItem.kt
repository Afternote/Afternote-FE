package com.afternote.core.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 비밀번호 규칙 안내 한 줄 — 불릿 + 문구 (#2191).
 *
 * 새 비밀번호를 받는 화면(회원가입 3단계·비밀번호 찾기의 변경·설정의 비밀번호 변경)이 같은 시안
 * `2383:16789` 의 안내 줄을 쓴다. 조합 규칙 문구는 `core_ui_password_rule_combination` 이고, 판정은
 * core:domain `NewPasswordRule` 이 갖는다.
 *
 * @param isSatisfied 규칙 충족 여부에 따라 색이 갈리는 줄만 넘긴다. null(기본)이면 상태 없는
 *   안내 줄이라 항상 강조색이다 — "이전에 사용한 적 없는 비밀번호가 안전합니다." 처럼 클라가
 *   판정할 수 없는 문구가 여기 해당한다.
 */
@Composable
public fun PasswordRuleItem(
    text: String,
    modifier: Modifier = Modifier,
    isSatisfied: Boolean? = null,
) {
    val color =
        if (isSatisfied == false) {
            AfternoteDesign.colors.gray5
        } else {
            AfternoteDesign.colors.b1
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            modifier = Modifier.clearAndSetSemantics {},
            style = AfternoteDesign.typography.captionLargeB,
            color = color,
        )
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeB,
            color = color,
        )
    }
}
