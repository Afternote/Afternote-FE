package com.afternote.core.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.common.util.KoreanConsonantUtil
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 인덱스 바가 그리는 섹션 라벨.
 *
 * [KoreanConsonantUtil.getInitialConsonant] 가 만드는 섹션 키 집합과 같아야 한다.
 * 어긋나면 그 섹션은 탭으로 점프도, 스크롤 하이라이트도 되지 않는다.
 */
private val CONSONANTS =
    listOf(
        'ㄱ',
        'ㄴ',
        'ㄷ',
        'ㄹ',
        'ㅁ',
        'ㅂ',
        'ㅅ',
        'ㅇ',
        'ㅈ',
        'ㅊ',
        'ㅋ',
        'ㅌ',
        'ㅍ',
        'ㅎ',
        KoreanConsonantUtil.NON_KOREAN_SECTION,
    )

@Composable
fun KoreanConsonantIndex(
    selectedConsonant: Char?,
    onConsonantSelect: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    var totalHeightPx by remember { mutableFloatStateOf(0f) }

    fun indexFromY(y: Float): Int =
        (y / totalHeightPx * CONSONANTS.size)
            .toInt()
            .coerceIn(0, CONSONANTS.lastIndex)

    Column(
        modifier =
            modifier
                .width(24.dp)
                .onSizeChanged { totalHeightPx = it.height.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onConsonantSelect(CONSONANTS[indexFromY(offset.y)])
                    }
                }.pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        onConsonantSelect(CONSONANTS[indexFromY(change.position.y)])
                    }
                },
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CONSONANTS.forEach { consonant ->
            val isSelected = consonant == selectedConsonant
            Box(
                modifier = Modifier.padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = consonant.toString(),
                    style = AfternoteDesign.typography.captionLargeR,
                    color =
                        if (isSelected) {
                            AfternoteDesign.colors.gray9
                        } else {
                            AfternoteDesign.colors.gray4
                        },
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun KoreanConsonantIndexPreview() {
    AfternoteTheme {
        KoreanConsonantIndex(
            selectedConsonant = 'ㄱ',
            onConsonantSelect = {},
        )
    }
}
