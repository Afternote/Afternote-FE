package com.afternote.feature.mindrecord.presentation.screen.memoryspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem
import com.android.tools.screenshot.PreviewTest

/**
 * 추억 공간은 `verticalScroll`·`Lazy*`·`Pager` 가 없다 — 세로가 모자라면 그대로 잘린다 (#1131).
 *
 * 상태 셋(기록 있음·0건·조회 실패)을 기본 크기와 좁은 화면([COMPACT_DEVICE_SPEC]) 양쪽에서
 * 고정한다. 0건과 실패는 문구뿐 아니라 **재시도 버튼 유무**로 갈리므로 따로 둔다.
 */
@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
internal fun memorySpaceScreenScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = previewMemories(),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun memorySpaceScreenCompactScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = previewMemories(),
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
internal fun memorySpaceScreenEmptyScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "아직 담긴 기록이 없어요.\n일기나 데일리 질문에 답하면 이곳에 쌓입니다.",
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun memorySpaceScreenEmptyCompactScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "아직 담긴 기록이 없어요.\n일기나 데일리 질문에 답하면 이곳에 쌓입니다.",
        )
    }
}

/** 조회 실패 — 0건과 달리 재시도 버튼이 함께 그려진다. */
@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
internal fun memorySpaceScreenErrorScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "기록을 불러오지 못했습니다.",
            onRetryClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun memorySpaceScreenErrorCompactScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = "기록을 불러오지 못했습니다.",
            onRetryClick = {},
        )
    }
}

/**
 * 네트워크 이미지는 렌더되지 않으므로 baseline 에는 자리와 텍스트만 남는다 — 잘림을 보는
 * 데에는 그것으로 충분하다.
 */
private fun previewMemories(): List<MemoryItem> =
    listOf(
        MemoryItem(1L, "https://example.com/1.jpg", "기억 1", "2024.11.11", "미리보기", listOf("태그")),
        MemoryItem(2L, "https://example.com/2.jpg", "기억 2", "2024.11.12", "미리보기", emptyList()),
        MemoryItem(3L, "https://example.com/3.jpg", "기억 3", "2024.11.13", "미리보기", emptyList()),
        MemoryItem(4L, "https://example.com/4.jpg", "기억 4", "2024.11.14", "미리보기", emptyList()),
    )
