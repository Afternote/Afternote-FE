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
    // 응답/요청 계약에 없는 필드라 타입은 nullable 로 두되 기본값은 두지 않는다.
    // 매퍼가 항상 명시적으로 넘기므로 생략이 조용히 통과할 자리가 없다 (#789).
    @SerialName("imageUrl") val imageUrl: String?,
)

/**
 * `PATCH /daily-questions/{id}` 요청. 선택 필드라 nullable 은 유지하되 기본값은 두지 않는다 —
 * 모든 생성 인자를 명시해야 "보내지 않음" 과 "명시적 null" 이 호출부 코드에서 구분된다 (#789).
 */
@Serializable
data class DailyQuestionUpdateRequestDto(
    @SerialName("content") val content: String?,
    @SerialName("isDraft") val isDraft: Boolean?,
    @SerialName("date") val date: String?,
    @SerialName("questionId") val questionId: Long?,
    @SerialName("imageUrl") val imageUrl: String?,
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
    // Swagger `DailyQuestionListResponse` 응답 계약에 없는 필드 — 서버가 주기 시작하면 쓰이고,
    // 아니면 계속 null. 계약에 없으므로 여기서만 기본값을 유지한다 (#789).
    @SerialName("imageUrl") val imageUrl: String? = null,
    // Swagger `DailyQuestionListResponse` 및 실서버 응답 모두 와이어 키는 `isDraft`.
    // 과거 QA logcat 에서 `draft` 로 관측된 적이 있어 대체 키도 함께 허용한다.
    //
    // 기본값 `false` 는 두지 않는다 — 서버가 항상 보내는 키라, 빠졌을 때 "임시저장 아님" 으로
    // 접히면 임시저장 글이 목록에 그대로 노출된다. 계약 누락은 파싱 실패로 드러나야 한다 (#789).
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean,
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
    // 종전 DTO 가 `answered`/`draft` 로 잡혀 있어 필수 필드 누락(MissingFieldException)으로
    // getToday() 가 항상 실패했다 (#548). 과도기 대비로 구 키는 계속 함께 받는다.
    //
    // 기본값은 두지 않는다 — 두 키 다 서버가 항상 보내므로, 기본값이 있으면 이번 같은 키
    // 불일치가 다시 나도 "미답변·임시저장 아님" 인 정상 응답으로 조용히 통과한다 (#789).
    @SerialName("isAnswered")
    @JsonNames("answered")
    val isAnswered: Boolean,
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean,
)
