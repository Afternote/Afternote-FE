package com.afternote.feature.mindrecord.domain.model

import java.time.LocalDate

enum class TodayMood {
    HAPPY,
    SOSO,
    SAD,
}

data class Diary(
    val diaryId: Long,
    val title: String,
    val content: String,
    /** 사용자가 고른 일기 날짜 (yyyy-MM-dd). 서버가 항상 채우는 값이다 (#789). */
    val date: String,
    val createdAt: String,
    /** 사용자가 직접 고른 오늘의 기분. 저장 컬럼이 필수라 응답에도 항상 있다 (#789). */
    val todayMood: TodayMood,
    /**
     * 목록 카드 썸네일. **서버 응답에 이 키가 없어 항상 null 이다** — 표시 단계에서 본문
     * HTML 의 첫 img 로 채운다 (#1024). 계약이 넓어지면 그때 서버 값이 우선한다.
     */
    val imageUrl: String? = null,
    val isDraft: Boolean = false,
    /** 이 기록을 전달받을 수신자 이름들. 상세 화면이 "수신인 OOO" 로 보여준다 (#759). */
    val receiverNames: List<String> = emptyList(),
)

data class DiaryList(
    val diaries: List<Diary>,
    val monthDiaryCount: Int,
    val weeklyDominantMood: TodayMood?,
)

data class DiaryCreatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    /** 이 일기를 전달받을 수신자 ID 목록. 미선택 시 빈 목록 — 서버가 "수신자 없음" 으로 본다. */
    val receiverIds: List<Long>,
    /**
     * 사용자가 고른 기록일. 서버는 미전송 시 오늘(Asia/Seoul)로 채우지만 작성 화면은 항상
     * 값을 갖고 있어 그대로 싣는다 (#1008).
     *
     * **미래 날짜는 서버가 400(code 2101)으로 거절한다.** 과거는 제한이 없다.
     */
    val date: LocalDate,
)

data class DiaryUpdatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    /**
     * 이 일기를 전달받을 수신자 ID 목록. 생성 경로와 같은 형태다 (#955).
     *
     * 서버 규칙: **null 이면 기존 수신자를 그대로 두고, 빈 목록이면 전체 해제**한다 (실측).
     * 그래서 "고른 게 없음" 을 빈 목록으로 보내면 안 된다 — 수신자를 건드리지 않은 편집이
     * 기존 지정을 지워 버린다.
     */
    val receiverIds: List<Long>? = null,
    /**
     * 기록일. **`null` 이면 서버가 기존 값을 유지한다** (`DiaryUpdateRequest.date` — 생략 시 유지).
     *
     * 화면에 뜬 날짜가 서버에서 온 것이거나 사용자가 직접 고른 것일 때만 싣는다. 프리필이
     * 날짜를 못 가져온 채로 «오늘» 을 실어 보내면 기록일이 조용히 오늘로 옮겨진다 — 수신자
     * 목록을 빈 목록으로 보내면 안 되는 것과 같은 종류의 함정이다 (#1008).
     */
    val date: LocalDate? = null,
)
