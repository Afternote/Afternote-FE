package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 일기 수정 요청의 와이어 계약 가드 (#955).
 *
 * 이어쓰기 등록은 `PATCH /diary/{id}` 를 타는데, 이 본문에 `receiverIds` 가 없어서
 * **수신자를 골라 등록해도 0명으로 발행**되고 화면은 성공으로 표시됐다. 반대로 `date` 는
 * 서버 스키마에 없는 필드인데 보내고 있었다(실측: 보내도 무시된다).
 *
 * 매퍼 반환값이 아니라 **직렬화된 본문**을 본다 — 실제로 서버가 받는 것이 그것이다.
 */
class DiaryUpdateRequestTest {
    /** 앱이 실제로 쓰는 설정과 같게 둔다 (core:network NetworkModule). */
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private fun bodyOf(receiverIds: List<Long>?) =
        json.encodeToString(
            DiaryUpdatePayload(
                title = "제목",
                content = "<p>본문</p>",
                isDraft = false,
                todayMood = TodayMood.HAPPY,
                receiverIds = receiverIds,
            ).toRequest(),
        )

    @Test
    fun `고른 수신자가 요청 본문에 실린다`() {
        assertTrue(bodyOf(listOf(17L, 18L)).contains("\"receiverIds\":[17,18]"))
    }

    @Test
    fun `수신자를 안 골랐으면 receiverIds 를 아예 보내지 않는다`() {
        // 서버는 빈 목록을 "전체 해제" 로 읽는다. 안 고른 것을 해제로 보내면 수신자를
        // 건드리지 않은 편집이 기존 지정을 지운다.
        assertFalse(bodyOf(null).contains("receiverIds"))
    }

    @Test
    fun `서버 스키마에 없는 date 는 보내지 않는다`() {
        assertFalse(bodyOf(null).contains("\"date\""))
    }

    @Test
    fun `나머지 필드는 그대로 실린다`() {
        val body =
            json.encodeToString(
                DiaryUpdatePayload(
                    title = "제목",
                    content = "<p>본문</p>",
                    isDraft = true,
                    todayMood = TodayMood.SAD,
                    receiverIds = listOf(17L),
                ).toRequest(),
            )

        assertTrue(body.contains("\"title\":\"제목\""))
        assertTrue(body.contains("\"content\":\"<p>본문</p>\""))
        assertTrue(body.contains("\"isDraft\":true"))
        assertTrue(body.contains("\"todayMood\":\"SAD\""))
        assertTrue(body.contains("\"receiverIds\":[17]"))
    }

    @Test
    fun `서버 스키마에 없는 imageUrl 도 보내지 않는다`() {
        // date 를 뺀 근거가 그대로 적용된다 — BE `DiaryUpdateRequest` 는
        // [title, content, isDraft, todayMood, receiverIds] 뿐이고 `Diary.imageUrl` 에
        // 대입하는 코드도 없다. 화면이 이미지를 붙인 편집에서만 실려 나가던 자리다.
        assertFalse(bodyOf(null).contains("\"imageUrl\""))
    }
}
