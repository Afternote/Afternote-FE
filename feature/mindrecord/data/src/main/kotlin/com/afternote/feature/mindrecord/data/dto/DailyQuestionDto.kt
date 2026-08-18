@file:OptIn(ExperimentalSerializationApi::class)

package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class DailyQuestionCreateRequestDto(
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("questionId") val questionId: Long,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionUpdateRequestDto(
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("questionId") val questionId: Long? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DailyQuestionListItemDto(
    // 서버는 사용자별 답변 레코드 ID 를 `userDailyQuestionId` 로 내려준다.
    // 도메인에서는 그대로 `dailyQuestionId` 로 받지만 와이어 키는 다름에 주의.
    // 노션 명세 예시는 `dailyQuestionId` 키를 사용 — 어느 쪽이 오더라도 파싱되도록 대체 키 허용.
    @SerialName("userDailyQuestionId")
    @JsonNames("dailyQuestionId")
    val dailyQuestionId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("imageUrl") val imageUrl: String? = null,
    // Swagger `DailyQuestionListResponse` 및 실서버 응답 모두 와이어 키는 `isDraft`.
    // 과거 QA logcat 에서 `draft` 로 관측된 적이 있어 대체 키도 함께 허용한다.
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean = false,
)

@Serializable
data class TodayDailyQuestionDto(
    @SerialName("questionId") val questionId: Long,
    // "Day N" 배너 표기용. **non-null 을 유지한다** — 현재 release 의 today 성공 응답은 할당된
    // 질문의 `day` 를 항상 채우므로, 기본값으로 낮추면 계약 누락을 정상 응답으로 숨기게 된다.
    // 표기 전용이라는 화면상의 중요도는 wire 값이 없을 수 있다는 근거가 아니다 (#789).
    //
    // Swagger 타입은 int64 지만 서비스 시작일 기준 일차라 Int 범위를 넘지 않는다.
    @SerialName("day") val day: Int,
    @SerialName("content") val content: String,
    // Swagger `DailyQuestionTodayResponse` 및 실서버 응답 모두 `isAnswered`/`isDraft`.
    // 종전 `answered`/`draft` 로 잡혀 있어 필수 필드 누락(MissingFieldException)으로
    // getToday() 가 항상 실패했다 (#548). 과도기 대비로 구 키도 함께 받고 기본값을 둔다.
    @SerialName("isAnswered")
    @JsonNames("answered")
    val isAnswered: Boolean = false,
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean = false,
)
