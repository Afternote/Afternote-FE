package com.afternote.feature.mindrecord.domain.testing

import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker

/**
 * [DiaryRepository] fake 정본. 규약은 [FakeDailyQuestionRepository] 와 같다 —
 * 기본은 메모리 저장소, 표현 못 하는 시나리오만 `onX` 람다로 갈아끼운다 (#1030).
 *
 * 조회는 `yearMonth` 로 거르지 **않는다**. 테스트가 넣는 일기는 대개 조회 대상 달의
 * 것이고, 달을 넘나드는 시나리오는 `onGetList` 로 직접 답하는 편이 읽기 쉽다.
 */
class FakeDiaryRepository(
    initialDiaries: List<Diary> = emptyList(),
    var onGetList: (suspend (String, Boolean?) -> Result<DiaryList>)? = null,
    var onCreate: (suspend (DiaryCreatePayload) -> Result<Unit>)? = null,
    var onUpdate: (suspend (Long, DiaryUpdatePayload) -> Result<Unit>)? = null,
    var onDelete: (suspend (Long) -> Result<Unit>)? = null,
    /**
     * 쓰기 성공을 알릴 변경 추적기 — 프로덕션에서는 data 계층이 이 자리에서 부른다.
     * fake 가 흉내 내지 않으면 목록 재조회 가드(#736)가 갱신을 건너뛴다 (#966 리뷰).
     */
    private val changeTracker: MindRecordChangeTracker? = null,
) : DiaryRepository {
    val diaries: MutableList<Diary> = initialDiaries.toMutableList()

    val listQueries = mutableListOf<ListQuery>()
    val createdPayloads = mutableListOf<DiaryCreatePayload>()
    val updatedPayloads = mutableListOf<Pair<Long, DiaryUpdatePayload>>()
    val deletedIds = mutableListOf<Long>()

    data class ListQuery(
        val yearMonth: String,
        val draftOnly: Boolean?,
    )

    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> {
        listQueries += ListQuery(yearMonth, draftOnly)
        onGetList?.let { return it(yearMonth, draftOnly) }
        val matching = diaries.filter { draftOnly == null || it.isDraft == draftOnly }
        return Result.success(
            DiaryList(
                diaries = matching,
                monthDiaryCount = matching.size,
                weeklyDominantMood = matching.firstOrNull()?.todayMood,
            ),
        )
    }

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> {
        createdPayloads += payload
        onCreate?.let { return it(payload) }
        changeTracker?.notifyChanged()
        return Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> {
        updatedPayloads += id to payload
        onUpdate?.let { return it(id, payload) }
        diaries.replaceAll { diary ->
            if (diary.diaryId == id) {
                // imageUrl 은 수정 요청 계약에 없다 (#955) — 기존 값을 유지한다.
                // date 는 2026-08-29 부터 계약에 있고 **생략(null)이면 기존 값 유지**다
                // (Afternote-BE#244, PR #262). 서버와 같은 규칙으로 흉내 낸다 (#1008).
                diary.copy(
                    title = payload.title,
                    content = payload.content,
                    todayMood = payload.todayMood,
                    isDraft = payload.isDraft,
                    date = payload.date?.toString() ?: diary.date,
                )
            } else {
                diary
            }
        }
        changeTracker?.notifyChanged()
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        deletedIds += id
        onDelete?.let { return it(id) }
        diaries.removeAll { it.diaryId == id }
        changeTracker?.notifyChanged()
        return Result.success(Unit)
    }

    companion object {
        /** 모든 호출을 실패시키고, 시나리오가 실제로 쓰는 것만 `onX` 로 연다. */
        fun strict(): FakeDiaryRepository =
            FakeDiaryRepository(
                onGetList = { _, _ -> unexpectedCall("DiaryRepository.getList") },
                onCreate = { unexpectedCall("DiaryRepository.create") },
                onUpdate = { _, _ -> unexpectedCall("DiaryRepository.update") },
                onDelete = { unexpectedCall("DiaryRepository.delete") },
            )
    }
}
