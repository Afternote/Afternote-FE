package com.afternote.feature.mindrecord.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 상세 화면 본문 블록 분할 가드 (#759).
 *
 * 시안(3814:18721)은 본문을 «문단 → 이미지 → 문단» 처럼 섞어 보여준다. 본문은 HTML
 * 조각이고 이미지는 그 안의 `img` 태그이므로, 태그를 경계로 잘라 순서를 지켜야 한다.
 *
 * `htmlToPlainText` 가 `HtmlCompat` 을 타서 Robolectric 이 필요하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordContentBlockTest {
    @Test
    fun `문단과 이미지가 본문 순서대로 나온다`() {
        val html = "<p>앞 문단</p><img src=\"https://cdn/a.png\" /><p>뒤 문단</p>"

        val blocks = html.toRecordContentBlocks()

        assertEquals(3, blocks.size)
        assertEquals("앞 문단", (blocks[0] as RecordContentBlock.Text).text)
        assertEquals("https://cdn/a.png", (blocks[1] as RecordContentBlock.Image).url)
        assertEquals("뒤 문단", (blocks[2] as RecordContentBlock.Text).text)
    }

    @Test
    fun `이미지가 없으면 문단만 남는다`() {
        // 시안의 "이미지 X" 변형 — 헤더도 이 결과로 갈린다.
        val blocks = "<p>글만 있는 본문</p>".toRecordContentBlocks()

        assertEquals(1, blocks.size)
        assertTrue(blocks.single() is RecordContentBlock.Text)
    }

    @Test
    fun `이미지 앞뒤의 빈 문단은 빈 줄로 남지 않는다`() {
        // 에디터가 이미지를 <p></p> 로 감싸 내보낸다 — 그대로 그리면 빈 줄이 생긴다.
        val html = "<p>본문</p><p><img src=\"https://cdn/a.png\" /></p>"

        val blocks = html.toRecordContentBlocks()

        assertEquals(2, blocks.size)
        assertEquals("본문", (blocks[0] as RecordContentBlock.Text).text)
        assertEquals("https://cdn/a.png", (blocks[1] as RecordContentBlock.Image).url)
    }

    @Test
    fun `이미지가 여러 장이면 모두 순서대로 남는다`() {
        val html = "<img src=\"https://cdn/1.png\"><p>사이</p><img src=\"https://cdn/2.png\">"

        val blocks = html.toRecordContentBlocks()

        assertEquals(
            listOf("https://cdn/1.png", "https://cdn/2.png"),
            blocks.filterIsInstance<RecordContentBlock.Image>().map { it.url },
        )
        assertEquals("사이", (blocks[1] as RecordContentBlock.Text).text)
    }

    @Test
    fun `src 가 빈 img 는 블록을 만들지 않는다`() {
        val blocks = "<p>본문</p><img src=\"\">".toRecordContentBlocks()

        assertEquals(1, blocks.size)
    }

    @Test
    fun `빈 본문은 블록이 없다`() {
        assertEquals(emptyList<RecordContentBlock>(), "<p></p>".toRecordContentBlocks())
    }
}
