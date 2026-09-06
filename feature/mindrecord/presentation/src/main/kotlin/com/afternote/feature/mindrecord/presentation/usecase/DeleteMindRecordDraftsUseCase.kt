package com.afternote.feature.mindrecord.presentation.usecase

import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * 임시저장 **일괄 삭제 정책** (#1693).
 *
 * 종전에는 `DraftListViewModel` 안에 있었다. UI 상태 갱신과 뒤섞여 있었지만 실제로 하는 일은
 * 두 저장소에 걸친 **파괴적 동작의 조율**이다 — 되돌릴 수 없고, 부분 실패가 정상 경로이며,
 * 어느 항목이 실패했는지를 «지금도 남아 있는가» 로 다시 판정해야 한다.
 *
 * ### 왜 실패 목록을 그대로 돌려주지 않는가
 *
 * 실패했지만 **재조회에서도 사라진 항목은 실패로 치지 않는다.** 이미 없는 것을 지우려다 404 가
 * 난 경우가 여기다 — 사용자가 원한 결과는 이뤄졌고, 남아 있지도 않은 항목을 「다시 선택해
 * 주세요」 라고 하면 「목록은 비었는데 1개 선택」 같은 상태가 된다.
 *
 * 그래서 삭제 결과만으로 끝내지 않고 **재조회 결과와 대조**한 뒤 판정한다. 이 대조가 이
 * 동작의 핵심이고, 삭제 호출 자체는 그 재료일 뿐이다.
 *
 * ### 계측을 여기서 하지 않는 이유
 *
 * 실패 원인([Outcome.Failure.cause])을 값으로 실어 올리고 기록은 호출부에 맡긴다. 계측은
 * 「무엇을 어느 화면에서」 라는 UI 맥락과 함께 남아야 의미가 있다 (#964 · #1693).
 */
class DeleteMindRecordDraftsUseCase
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) {
        /**
         * [targets] 를 **병렬로** 지우고, 실패 전건과 그중 아직 남아 있는 것을 나눠 돌려준다.
         *
         * @param targets 지울 대상. 비어 있으면 아무 것도 하지 않고 [Outcome.Deleted] 를 돌린다.
         * @param survivorsAfterDelete 삭제 뒤 다시 조회한 목록의 키. 재조회 자체가 실패했으면
         *   `null` — 그때는 무엇이 남았는지 알 수 없어 대조할 수 없으므로 [Outcome.Unknown] 이다.
         * @param onFailures 실패 전건을 **재조회 전에** 한 번 넘긴다. 계측이 여기 걸린다.
         *   반환값으로만 넘기면 재조회 도중 스코프가 취소될 때(사용자가 화면을 벗어날 때)
         *   삭제는 이미 서버에 반영됐는데 실패는 한 건도 안 남는 창이 열린다 (#1693 리뷰).
         *   보고 주체는 여전히 호출부다 — UseCase 는 계측을 알지 않는다.
         */
        suspend fun delete(
            targets: List<Target>,
            survivorsAfterDelete: suspend () -> Set<Target>?,
            onFailures: (List<Failure>) -> Unit = {},
        ): Outcome {
            if (targets.isEmpty()) return Outcome.Deleted(failures = emptyList(), remaining = emptyList())

            // 항목별 결과를 항목과 짝지어 받는다 — 무엇이 실패했는지 알아야 다시 선택해 줄 수 있다.
            val results =
                coroutineScope {
                    targets.map { target -> async { target to deleteOne(target) } }.awaitAll()
                }
            val failures =
                results.mapNotNull { (target, result) ->
                    result.exceptionOrNull()?.let { Failure(target = target, cause = it) }
                }

            // 재조회 **전에** 넘긴다 — 그 사이 취소되면 실패가 통째로 사라진다.
            onFailures(failures)

            val survivors = survivorsAfterDelete() ?: return Outcome.Unknown(failures = failures)
            return Outcome.Deleted(
                failures = failures,
                remaining = failures.filter { it.target in survivors },
            )
        }

        private suspend fun deleteOne(target: Target): Result<Unit> =
            when (target.category) {
                Category.Diary -> diaryRepository.delete(target.id)
                Category.DailyQuestion -> dailyQuestionRepository.delete(target.id)
            }

        /**
         * 지울 대상의 **안정된 식별자**.
         *
         * 두 저장소의 id 는 서로 독립이라 숫자가 겹칠 수 있다 — 종류를 함께 들지 않으면
         * 일기 3번과 데일리질문 3번이 같은 항목으로 취급된다.
         */
        data class Target(
            val category: Category,
            val id: Long,
        )

        /** 실제로 지울 수 있는 종류. 화면의 «전체» 필터 라벨은 여기 없다 — 항목이 아니라 필터다. */
        enum class Category { Diary, DailyQuestion }

        data class Failure(
            val target: Target,
            val cause: Throwable,
        )

        sealed interface Outcome {
            /**
             * 이번 삭제에서 난 **실패 전건**. 재조회 대조와 무관하게 그대로 싣는다.
             *
             * 계측이 보는 것은 이쪽이다. 「이미 사라진 항목의 404」처럼 화면에 안 보이는 실패일수록
             * 계측이 유일한 흔적이라, 대조로 걸러 내면 콘솔에서도 사라진다 (#964·#1693).
             */
            val failures: List<Failure>

            /**
             * 삭제와 재조회가 모두 끝났다.
             *
             * 화면이 쓰는 것은 [remaining] 이다 — 비어 있으면 사용자가 원한 결과가 전부 이뤄졌다.
             * [failures] 와 갈리는 이유는 위 KDoc 에 있다.
             */
            data class Deleted(
                override val failures: List<Failure>,
                /** [failures] 중 재조회 목록에 **아직 남아 있는** 것. 다시 선택해 줄 대상이다. */
                val remaining: List<Failure>,
            ) : Outcome

            /**
             * 재조회가 실패해 무엇이 남았는지 모른다. 목록을 그릴 수 없으므로 화면은 오류로 간다.
             * 그래도 [failures] 는 계측을 위해 그대로 싣는다.
             */
            data class Unknown(
                override val failures: List<Failure>,
            ) : Outcome
        }
    }
