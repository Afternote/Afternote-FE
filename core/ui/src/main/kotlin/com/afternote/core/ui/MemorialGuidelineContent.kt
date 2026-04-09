package com.afternote.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 추모 가이드라인 공통 세로 레이아웃(섹션 순서·간격만 담당).
 * 에디터·수신자(View) 화면에서 동일 골격을 쓰기 위해 분리했습니다.
 *
 * 슬롯은 data class가 아닌 **컴포저블의 직접 파라미터**로 열어, 호출 지점 기준 리컴포지션 추적이 끊기지 않게 합니다.
 *
 * @param sectionSpacing 섹션 사이 세로 간격
 * @param trailingSpacerHeight 하단 여백(편집 화면 등). [videoContent] 직후에만 두어 [spacedBy] 간격에 섞이지 않게 합니다.
 */
@Composable
fun MemorialGuidelineContent(
    introContent: @Composable () -> Unit,
    photoContent: @Composable () -> Unit,
    playlistContent: @Composable () -> Unit,
    lastWishContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sectionSpacing: Dp = 32.dp,
    trailingSpacerHeight: Dp = 0.dp,
    recipientContent: @Composable () -> Unit = {},
    videoContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
    ) {
        introContent()
        photoContent()
        playlistContent()
        lastWishContent()
        recipientContent()
        Column(modifier = Modifier.fillMaxWidth()) {
            videoContent()
            if (trailingSpacerHeight > 0.dp) {
                Spacer(modifier = Modifier.height(trailingSpacerHeight))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MemorialGuidelineContentPreview() {
    AfternoteTheme {
        MemorialGuidelineContent(
            introContent = {},
            photoContent = {},
            playlistContent = {},
            lastWishContent = {},
            recipientContent = {},
            videoContent = {},
        )
    }
}
