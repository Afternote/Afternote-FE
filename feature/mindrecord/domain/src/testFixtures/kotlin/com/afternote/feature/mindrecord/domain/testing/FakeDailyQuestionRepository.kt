package com.afternote.feature.mindrecord.domain.testing

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker

/**
 * [DailyQuestionRepository] fake 정본.
 *
 * 기본 동작은 **메모리 저장소**다 — 생성하면 [answers] 에 쌓이고, 수정·삭제가 그 목록에
 * 반영되며, 조회는 그 상태를 돌려준다. 호출은 전부 기록되므로 테스트는 사후에 단언한다.
 *
 * 기본 동작으로 안 되는 시나리오(경합을 만들려고 조회를 붙잡아 두기, 특정 호출을 실패로
 * 밀어 넣기, "이 시나리오에서 호출되면 안 됨" 을 그 자리에서 터뜨리기)는 `onX` 람다로
 * 갈아끼운다. 람다가 `null` 이면 기본 동작이다.
 *
 * 시나리오마다 클래스를 새로 만들지 않는다 — 계약이 바뀔 때 고칠 곳이 갈라지는 게
 * androidTest 컴파일 파손의 원인이었다 (#936, #1022, #1030).
 */
class FakeDailyQuestionRepository(
    initialAnswers: List<DailyQuestion> = emptyList(),
    var today: TodayDailyQuestion = DEFAULT_TODAY,
    var onGetList: (suspend (String?, Boolean?) -> Result<List<DailyQuestion>>)? = null,
    var onGetToday: (suspend () -> Result<TodayDailyQuestion>)? = null,
    var onCreate: (suspend (DailyQuestionCreatePayload) -> Result<Long>)? = null,
    var onUpdate: (suspend (Long, DailyQuestionUpdatePayload) -> Result<Long>)? = null,
    var onDelete: (suspend (Long) -> Result<Unit>)? = null,
    /**
     * 쓰기 성공을 알릴 변경 추적기. **프로덕션에서는 data 계층(`DailyQuestionRepositoryImpl`)이
     * 이 자리에서 `notifyChanged()` 를 부른다** — fake 가 그걸 흉내 내지 않으면 목록 화면의
     * 재조회 가드(#736)가 «데이터가 안 바뀌었다» 로 보고 갱신을 건너뛴다. 그러면 «작성하고
     * 돌아왔는데 목록이 그대로»(#520) 를 잡는 테스트가 조용히 침묵한다 (#966 리뷰).
     *
     * 재조회를 보지 않는 시나리오는 넘기지 않아도 된다.
     */
    private val changeTracker: MindRecordChangeTracker? = null,
) : DailyQuestionRepository {
    /** 메모리 저장소. 테스트가 직접 들여다보거나 조작해도 된다. */
    val answers: MutableList<DailyQuestion> = initialAnswers.toMutableList()

    val listQueries = mutableListOf<ListQuery>()
    val createdPayloads = mutableListOf<DailyQuestionCreatePayload>()
    val updatedPayloads = mutableListOf<Pair<Long, DailyQuestionUpdatePayload>>()
    val deletedIds = mutableListOf<Long>()

    var getTodayCalls: Int = 0
        private set

    /** 다음에 붙일 식별자. 생성 fake 가 돌려주는 `userDailyQuestionId` 자리다 (#573). */
    var nextCreatedId: Long = FIRST_CREATED_ID

    data class ListQuery(
        val date: String?,
        val draftOnly: Boolean?,
    )

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> {
        listQueries += ListQuery(date, draftOnly)
        onGetList?.let { return it(date, draftOnly) }
        // `draftOnly` 를 생략하면 서버는 제출 완료만 내려준다 — 그 계약을 그대로 흉내 낸다.
        val matching =
            when (draftOnly) {
                null -> answers.filterNot { it.isDraft }
                else -> answers.filter { it.isDraft == draftOnly }
            }
        return Result.success(matching)
    }

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        getTodayCalls += 1
        onGetToday?.let { return it() }
        return Result.success(today)
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
        createdPayloads += payload
        onCreate?.let { return it(payload) }
        val id = nextCreatedId
        nextCreatedId += 1
        answers +=
            DailyQuestion(
                dailyQuestionId = id,
                title = today.content,
                content = payload.content,
                createdAt = DEFAULT_CREATED_AT,
                isDraft = payload.isDraft,
            )
        today = today.copy(isAnswered = !payload.isDraft, isDraft = payload.isDraft)
        changeTracker?.notifyChanged()
        return Result.success(id)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> {
        updatedPayloads += id to payload
        onUpdate?.let { return it(id, payload) }
        answers.replaceAll { answer ->
            if (answer.dailyQuestionId == id) {
                answer.copy(
                    content = payload.content ?: answer.content,
                    isDraft = payload.isDraft ?: answer.isDraft,
                )
            } else {
                answer
            }
        }
        changeTracker?.notifyChanged()
        return Result.success(id)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        deletedIds += id
        onDelete?.let { return it(id) }
        answers.removeAll { it.dailyQuestionId == id }
        changeTracker?.notifyChanged()
        return Result.success(Unit)
    }

    companion object {
        const val FIRST_CREATED_ID = 1L
        private const val DEFAULT_CREATED_AT = "2026-08-22"
        private val DEFAULT_TODAY = TodayDailyQuestion(1L, 1, "오늘의 질문", false)

        /**
         * 모든 호출을 그 자리에서 실패시키는 fake. 시나리오가 실제로 쓰는 것만 `onX` 로 연다.
         *
         * 메모리 저장소 기본 동작이 "안 불릴 줄 알았던 호출" 을 조용히 받아 주면, 종전
         * 시나리오별 fake 가 `error("호출되면 안 됨")` 으로 지키던 경계가 사라진다.
         */
        fun strict(): FakeDailyQuestionRepository =
            FakeDailyQuestionRepository(
                onGetList = { _, _ -> unexpectedCall("DailyQuestionRepository.getList") },
                onGetToday = { unexpectedCall("DailyQuestionRepository.getToday") },
                onCreate = { unexpectedCall("DailyQuestionRepository.create") },
                onUpdate = { _, _ -> unexpectedCall("DailyQuestionRepository.update") },
                onDelete = { unexpectedCall("DailyQuestionRepository.delete") },
            )
    }
}
