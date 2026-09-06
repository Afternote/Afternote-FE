package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 홈 탭 MEMORIES 카드.
 *
 * 시안(홈_수신자지정완료 57:8186)의 질문·답변은 **예시값**이라 문자열 리소스에 그대로 넣어
 * 두면 기록이 0건인 사용자에게도 남의 답변처럼 보인다 (#559). 실제 기록을 받아 표시한다.
 *
 * **0건 상태는 아직 시안에 없다** (Figma 카드 노드 `109:15028` 에 해당 변형 없음).
 * 임의 문구를 넣으면 미확정 상태가 제품 동작으로 굳으므로, 표시할 기록이 없으면
 * 질문·답변 줄을 아예 그리지 않는다. 디자인 확정 시 이 자리에 변형을 붙이면 된다.
 *
 * @param question 최근 기록의 제목(데일리질문이면 질문 원문). null 이면 표시할 기록이 없다.
 * @param answer 그 기록의 본문 미리보기.
 * @param onReadAgainClick "그날의 기록 다시 읽기" 목적지 — 카드가 가리키는 그 기록의 상세.
 *   **`null` 이면 버튼 자체를 그리지 않는다** (#793 리뷰). 열 기록이 없는데 버튼을 남기면
 *   문구가 약속한 「그날의 기록」이 아닌 곳으로 보내게 된다.
 *
 *   (옛 메모) 최종 확정은 아직이다 — Figma 의
 *   이 버튼에 프로토타입 연결이 없다. 다만 비워 두면 버튼이 클릭을 삼켜 카드 안에서 버튼만
 *   죽은 영역이 되므로, 홈 호출부는 카드 자신의 목적지(추억 공간)를 물려준다 (#793).
 */
@Composable
fun MemoriesCard(
    modifier: Modifier = Modifier,
    question: String? = null,
    answer: String? = null,
    onReadAgainClick: (() -> Unit)?,
) {
    val cardShape = RoundedCornerShape(8.dp)
    val buttonShape = RoundedCornerShape(20.dp)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, color = AfternoteDesign.colors.gray3, shape = cardShape)
                .clip(cardShape),
    ) {
        Image(
            painter = painterResource(R.drawable.mindrecord_img),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 0.7f }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0.0f to Color.Transparent,
                                            0.5f to Color.White.copy(alpha = 0.2f),
                                            1.0f to Color(0xFFF8F8F7),
                                        ),
                                ),
                        )
                    },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            if (question != null) {
                Text(
                    text = question,
                    style = AfternoteDesign.typography.bodySmallR,
                    color = AfternoteDesign.colors.gray8,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (answer != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = answer,
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (question != null || answer != null) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // **열 기록이 없으면 버튼을 그리지 않는다** (#793 리뷰). 문구가 「그날의 기록」을
            // 약속하는데 0건에서 다른 곳으로 보내면 그 약속이 깨진다.
            if (onReadAgainClick != null) {
                Row(
                    modifier =
                        Modifier
                            .border(width = 1.dp, color = AfternoteDesign.colors.gray3, shape = buttonShape)
                            .clip(buttonShape)
                            .background(color = AfternoteDesign.colors.gray2)
                            .clickable(role = Role.Button, onClick = onReadAgainClick)
                            .padding(horizontal = 17.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.mindrecord_read_again),
                        style = AfternoteDesign.typography.inter,
                        color = AfternoteDesign.colors.gray7,
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    RightArrowIcon(
                        modifier = Modifier.size(width = 4.dp, height = 7.dp),
                        tint = AfternoteDesign.colors.gray5,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "기록 있음")
@Composable
private fun MemoriesCardPreview() {
    AfternoteTheme {
        MemoriesCard(
            question = "내 인생에서 가장 소중했던 순간은?",
            answer = "아이가 태어났을 때...",
            onReadAgainClick = {},
        )
    }
}

/** 기록 0건 — 질문·답변 줄도, 「다시 읽기」 버튼도 없다. 열 기록이 없기 때문이다 (#559 · #793). */
@Preview(showBackground = true, name = "기록 0건")
@Composable
private fun MemoriesCardEmptyPreview() {
    AfternoteTheme {
        MemoriesCard(onReadAgainClick = null)
    }
}
