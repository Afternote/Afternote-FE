@file:OptIn(ExperimentalSerializationApi::class)

package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * `POST /daily-questions` 요청 (Swagger `DailyQuestionAnswerRequest`).
 *
 * **`imageUrl` 필드는 없다.** 요청·응답 계약 어디에도 없고, 보내도 서버가 무시한다
 * (2026-08-23 실측). 본문 이미지는 [content] HTML 의 `img` 태그로 담긴다 (#549).
 */
@Serializable
data class DailyQuestionCreateRequestDto(
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("questionId") val questionId: Long,
)

/** `PATCH /daily-questions/{id}` 요청. [DailyQuestionCreateRequestDto] 와 같은 이유로 `imageUrl` 이 없다. */
@Serializable
data class DailyQuestionUpdateRequestDto(
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("questionId") val questionId: Long? = null,
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
    // `imageUrl` 은 응답 계약에 없고 실서버도 키 자체를 내려주지 않는다 — 항상 null 인
    // 필드를 읽느라 목록 카드 썸네일이 영영 뜨지 않았다. 썸네일은 표시 단계에서
    // `content` 의 첫 img 로 뽑는다 (#549).
    // Swagger `DailyQuestionListResponse` 및 실서버 응답 모두 와이어 키는 `isDraft`.
    // 과거 QA logcat 에서 `draft` 로 관측된 적이 있어 대체 키도 함께 허용한다.
    //
    // 기본값 `false` 는 두지 않는다 — 서버가 항상 보내는 키라, 빠졌을 때 "임시저장 아님" 으로
    // 접히면 임시저장 글이 목록에 그대로 노출된다. 계약 누락은 파싱 실패로 드러나야 한다 (#789).
    // 기본값을 두지 않는다 — 키가 빠지면 false 로 접혀 임시저장이 목록에 샌다 (#789).
    @SerialName("isDraft")
    @JsonNames("draft")
    val isDraft: Boolean,
    // 상세 화면의 "수신인 OOO" 표시용 (#759).
    @SerialName("receivers") val receivers: List<MindRecordReceiverDto>,
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

/**
 * 생성·수정 응답 (`DailyQuestionAnswerResponse` 실측, 2026-08-23).
 *
 * 종전에는 `BaseResponse<Unit>` 으로 받아 방금 만든 답변의 식별자를 버렸다. 그러면 저장
 * 직후 그 레코드를 가리키려고 목록을 다시 조회해 추측으로 찾아야 한다 (#573).
 *
 * `userDailyQuestionId`("내 답변")와 요청의 `questionId`("질문")는 의미가 다르다.
 * 관찰된 값이 같더라도 섞어 쓰면 안 된다.
 */
@Serializable
data class DailyQuestionAnswerResponseDto(
    @SerialName("userDailyQuestionId") val userDailyQuestionId: Long,
    // 기본값을 두지 않는다 — 서버가 항상 채우는 값이라, 기본값이 있으면 키 누락과 계약
    // 변경이 파싱 실패가 아니라 «정상적인 빈 값» 으로 바뀐다. 특히 isDraft 가 false 로
    // 접히면 임시저장이 정식 답변으로 보인다 (#789).
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
)
