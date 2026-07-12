package com.afternote.afternote_fe.usecase

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.HomeSummary
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.YearMonth
import javax.inject.Inject

/**
 * 홈 화면이 필요로 하는 필드를 여러 Repository에서 병렬 조립한다.
 *
 * 서버에 단일 `/home/summary` 엔드포인트가 존재하지 않아 클라이언트 합성으로 대체.
 * 프로필/수신자 조회는 필수(실패 시 전체 실패), 일기 카운트는 보조(실패 시 0 폴백)로
 * 처리해 카운트 호출 한 건의 실패 때문에 홈 화면 자체가 깨지지 않도록 한다.
 *
 * UseCase는 통상 `core:domain`에 위치하나, 이 조합은 `feature/mindrecord/domain`을 끌어와야
 * 하므로 backwards 의존을 피하기 위해 `app` 모듈에 둔다.
 */
class GetHomeSummaryUseCase
    @Inject
    constructor(
        private val userRepository: UserRepository,
        private val diaryRepository: DiaryRepository,
    ) {
        suspend operator fun invoke(): Result<HomeSummary> =
            runCatching {
                coroutineScope {
                    val thisMonth = YearMonth.now().toString() // yyyy-MM
                    val profileDeferred = async { userRepository.getMyProfile() }
                    val receiversDeferred = async { userRepository.getReceivers() }
                    // 백엔드가 `yearMonth` 를 필수 쿼리로 요구하므로 이번 달로 호출.
                    // 결과적으로 카운트는 "이번 달 작성된 일기 수" 의미가 됨.
                    val diaryDeferred = async { diaryRepository.getList(yearMonth = thisMonth) }

                    val profile = profileDeferred.await()
                    val receivers = receiversDeferred.await()
                    val diaryCount = diaryDeferred.await().getOrNull()?.monthDiaryCount ?: 0

                    HomeSummary(
                        userName = profile.name,
                        isRecipientDesignated = receivers.isNotEmpty(),
                        diaryCategoryCount = diaryCount,
                    )
                }
            }
    }
