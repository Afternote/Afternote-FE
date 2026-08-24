package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel

/**
 * "남기신 말씀" 섹션.
 *
 * 작성자가 남긴 말씀은 제목·본문 한 쌍이 여러 개일 수 있고, 시안은 그 하나하나를 **각각의 카드**로 그린다
 * ([Figma 변경 시안](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/%EC%95%A0%ED%94%84%ED%84%B0%EB%85%B8%ED%8A%B8--new-?node-id=3628-17591)).
 * [blocks] 가 비어 있으면 빈 카드 하나에 `feature_afternote_detail_no_message` 문구를 gray5 로 표시한다.
 */
@Composable
fun MessageSection(
    blocks: List<MessageBlockUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailSectionHeader(
            iconResId = R.drawable.feature_afternote_ic_leave_message_header,
            label = stringResource(R.string.feature_afternote_detail_section_message),
        )

        if (blocks.isEmpty()) {
            DetailCard {
                MessageBlockRow {
                    Text(
                        text = stringResource(R.string.feature_afternote_detail_no_message),
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray5,
                    )
                }
            }
        } else {
            blocks.forEach { block ->
                DetailCard {
                    MessageBlockRow {
                        if (block.title.isNotBlank()) {
                            Text(
                                text = block.title,
                                style = AfternoteDesign.typography.bodyLargeR,
                                color = AfternoteDesign.colors.gray8,
                            )
                        }
                        Text(
                            text = block.body,
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray8,
                        )
                    }
                }
            }
        }
    }
}

/** 인용 부호 + 본문 열. 카드 하나의 내부 배치는 빈 상태와 블록 표시가 동일하다. */
@Composable
private fun MessageBlockRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.feature_afternote_ic_leave_message_card),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray4,
            modifier = Modifier.size(15.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageSectionPreview() {
    AfternoteTheme {
        MessageSection(
            blocks =
                listOf(
                    MessageBlockUiModel(
                        title = "가족에게",
                        body = "소중한 사람들에게 남기는 마지막 메시지입니다.",
                    ),
                    MessageBlockUiModel(
                        body = "이 계정에는 우리 가족 여행 사진이 많아. 계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
                    ),
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageSectionEmptyPreview() {
    AfternoteTheme {
        MessageSection(blocks = emptyList())
    }
}
