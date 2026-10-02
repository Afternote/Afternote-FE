package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayMood
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 임시저장의 「기분 미선택」이 와이어까지 그대로 가는지 (#1065).
 *
 * 종전 FE 는 미선택을 `SOSO` 로 채워 보냈다. 그러면 **고른 적 없는 값이 사용자 데이터가
 * 된다** — 이어쓰기로 열면 「그냥그래」가 이미 선택돼 보이고, 주간리포트 기분 집계와 감정
 * 분석 입력에도 들어간다. 그래서 폴백을 걷었는데, 그때는 서버가 임시저장에도 기분을
 * 요구해서 화면이 미선택 자체를 막아야 했다.
 *
 * `Afternote-BE#243` → PR #267(2026-08-30 머지)이 그 검증을 걷었다. 이제 미선택은
 * **명시적 null** 로 나간다 — 키를 생략하지 않는다. 서버 규칙상 생략과 null 이 같더라도,
 * 「고르지 않았다」를 값으로 말하는 편이 나중에 두 의미가 갈릴 때 안전하다.
 *
 * 매퍼 반환값이 아니라 **직렬화된 본문**을 본다 — 실제로 서버가 받는 것이 그것이다.
 */
class DiaryDraftMoodRequestTest {
    /** 앱이 실제로 쓰는 설정과 같게 둔다 (core:network NetworkModule). */
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `기분을 안 고른 임시저장은 todayMood 를 null 로 보낸다`() {
        val body = json.encodeToString(createPayload(mood = null).toRequest())

        assertTrue(body, body.contains("\"todayMood\":null"))
        assertTrue("임시저장 표시가 빠졌다: $body", body.contains("\"isDraft\":true"))
    }

    @Test
    fun `기분을 고른 임시저장은 그 값을 그대로 보낸다`() {
        val body = json.encodeToString(createPayload(mood = TodayMood.SAD).toRequest())

        assertTrue(body, body.contains("\"todayMood\":\"SAD\""))
    }

    @Test
    fun `이어쓰기 수정도 미선택을 null 로 보낸다`() {
        // 이어쓰기는 PATCH 를 탄다 — 생성 경로만 열고 수정 경로를 닫아 두면 이어쓰다
        // 기분을 지운 임시저장을 저장할 수 없다.
        val body =
            json.encodeToString(
                DiaryUpdatePayload(
                    title = "쓰다 만 제목",
                    content = "",
                    isDraft = true,
                    todayMood = null,
                    receiverIds = null,
                ).toRequest(),
            )

        assertTrue(body, body.contains("\"todayMood\":null"))
    }

    private fun createPayload(mood: TodayMood?) =
        DiaryCreatePayload(
            title = "쓰다 만 제목",
            content = "",
            isDraft = true,
            todayMood = mood,
            receiverIds = emptyList(),
            date = LocalDate.of(2026, 9, 7),
        )
}
