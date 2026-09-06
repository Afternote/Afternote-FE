package com.afternote.feature.home.presentation.usecase

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.home.presentation.usecase.HomeSummary
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.usecase.GetWeeklyRecordCountUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.time.YearMonth
import javax.inject.Inject

/**
 * 홈 화면이 필요로 하는 필드를 여러 Repository에서 병렬 조립한다.
 *
 * 서버에 단일 `/home/summary` 엔드포인트가 존재하지 않아 클라이언트 합성으로 대체.
 * 프로필/수신자 조회는 필수(실패 시 전체 실패), 일기 카운트·오늘의 질문은 보조(실패 시
 * 0/null 폴백)로 처리해 보조 호출 한 건의 실패 때문에 홈 화면 자체가 깨지지 않도록 한다.
 *
 * UseCase는 통상 domain 레이어에 위치하나, 이 조합은 `feature/mindrecord/domain`과
 * `core:common`(runCatchingCancellable)을 함께 끌어와야 해서 domain 패키지에 두면
 * 레이어 가드(LayerDependencyKonsistTest, 비-core:model 코어 금지)에 걸린다.
 * 홈 전용 조합 로직이므로 홈 presentation 모듈에 둔다.
 */
class GetHomeSummaryUseCase
    @Inject
    constructor(
        private val myProfileRepository: MyProfileRepository,
        private val userReceiverRepository: UserReceiverRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
        private val getWeeklyRecordCount: GetWeeklyRecordCountUseCase,
    ) {
        suspend operator fun invoke(): Result<HomeSummary> =
            runCatchingCancellable {
                coroutineScope {
                    val profileDeferred = async { myProfileRepository.getMyProfile() }
                    val receiversDeferred = async { userReceiverRepository.getReceivers() }
                    val todayQuestionDeferred = async { dailyQuestionRepository.getToday() }
                    // 예산을 **async 안에서** 건다. 밖에서 걸면 `await()` 만 끊기고 이 async 는
                    // 여전히 coroutineScope 의 자식이라, scope 가 그 자식을 끝까지 기다린다 —
                    // 타임아웃을 걸고도 60초를 그대로 서 있게 된다(가드 테스트가 잡았다).
                    val weeklyCountDeferred =
                        async { withTimeoutOrNull(WEEKLY_COUNT_BUDGET_MILLIS) { getWeeklyRecordCount() } }

                    val profile = profileDeferred.await()
                    val receivers = receiversDeferred.await()
                    val todayQuestionContent = todayQuestionDeferred.await().getOrNull()?.content
                    // 실패를 0 으로 접지 않는다 — «기록이 없음» 과 «못 불러옴» 은 다르다 (#562).
                    //
                    // **주간 수는 홈 전체를 붙잡지 못하게 한다.** 이 값만 `/mind-record` 에서
                    // 오는데 그 엔드포인트가 병적으로 느려(#1122), 읽기 타임아웃 60초를 그대로
                    // 기다리면 이름·오늘의 질문·NEXT STEP 이 250ms 에 도착해 있어도 홈 전체가
                    // 그 동안 shimmer 로 남는다. 실기동 실측: 60,179ms 뒤 SocketTimeout.
                    //
                    // 예산을 넘기면 «못 불러옴»(null) 과 같이 다룬다 — 그리드가 이미 그 상태를
                    // 그리므로 새 상태가 늘지 않고, 홈은 나머지 응답 속도로 뜬다.
                    val weeklyRecordCount = weeklyCountDeferred.await()?.getOrNull()

                    HomeSummary(
                        userName = profile.name,
                        isRecipientDesignated = receivers.isNotEmpty(),
                        todayQuestionContent = todayQuestionContent,
                        weeklyRecordCount = weeklyRecordCount,
                    )
                }
            }
    }

/**
 * 주간 기록 수를 기다려 줄 예산. 홈의 나머지 조회는 실측 250ms 안팎이라 그보다 넉넉하되,
 * 사용자가 «멈췄다» 고 느끼기 전에 끝난다.
 */
private const val WEEKLY_COUNT_BUDGET_MILLIS = 3_000L
