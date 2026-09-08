package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.presentation.util.RecordContentBlock
import com.afternote.feature.mindrecord.presentation.util.toRecordContentBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 상세 화면 헤더 이미지 선택 규칙 (#759).
 *
 * 시안 4종은 첨부 이미지 유무로만 갈린다. 서버는 "대표 이미지" 를 따로 주지 않으므로
 * 본문 HTML 에서 **첫 이미지**를 뽑아 헤더로 쓰고, 하나도 없으면 그라데이션 변형이 된다.
 *
 * 본문 분할이 `HtmlCompat` 을 타서 Robolectric 이 필요하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordDetailHeroImageTest {
    @Test
    fun `본문 첫 이미지를 헤더로 쓴다`() {
        val blocks =
            (
                "<p>앞 문단입니다.</p>" +
                    "<img src=\"https://cdn.example.com/first.png\" />" +
                    "<img src=\"https://cdn.example.com/second.png\" />"
            ).toRecordContentBlocks()

        assertEquals("https://cdn.example.com/first.png", blocks.firstImageUrl())
    }

    @Test
    fun `이미지가 글 중간에 있어도 헤더로 올린다`() {
        // 헤더는 본문 순서와 무관하다 — 첫 문단 뒤에 붙은 사진도 시안의 "이미지 O" 변형이다.
        val blocks =
            "<p>앞 문단</p><img src=\"https://cdn.example.com/mid.png\" /><p>뒤 문단</p>"
                .toRecordContentBlocks()

        assertEquals("https://cdn.example.com/mid.png", blocks.firstImageUrl())
    }

    @Test
    fun `이미지가 없으면 헤더 이미지도 없다`() {
        val blocks = "<p>글만 있는 본문입니다.</p>".toRecordContentBlocks()

        assertNull(blocks.firstImageUrl())
        assertEquals(1, blocks.count { it is RecordContentBlock.Text })
    }

    @Test
    fun `본문이 비어도 터지지 않는다`() {
        assertNull("".toRecordContentBlocks().firstImageUrl())
    }

    @Test
    fun `src 가 빈 img 는 헤더로 쓰지 않는다`() {
        // 빈 src 를 그대로 넘기면 헤더가 "이미지 O" 변형으로 그려진 뒤 영원히 비어 있다.
        assertNull("<p>본문</p><img src=\"\" />".toRecordContentBlocks().firstImageUrl())
    }
}
