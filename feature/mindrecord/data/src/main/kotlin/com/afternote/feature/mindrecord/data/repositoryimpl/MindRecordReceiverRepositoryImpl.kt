package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.mapper.toDomain
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class MindRecordReceiverRepositoryImpl
    @Inject
    constructor(
        private val api: MindRecordReceiverApiService,
    ) : MindRecordReceiverRepository {
        override suspend fun getAll(): Result<ReceiverMindRecords> =
            runCatchingCancellable {
                coroutineScope {
                    val dailyQuestionsDeferred = async { api.getReceiverDailyQuestions().requireData() }
                    val diariesDeferred = async { api.getReceiverDiaries().requireData() }

                    ReceiverMindRecords(
                        dailyQuestions = dailyQuestionsDeferred.await().dailyQuestions.map { it.toDomain() },
                        diaries = diariesDeferred.await().diaries.map { it.toDomain() },
                    )
                }
            }.mapReceiverFailure()
    }

/** 전달 조건 미충족 — `403 {"code":2009}` (실서버 실측, 2026-08-23). */
private const val CODE_DELIVERY_CONDITION_NOT_MET = 2009

/**
 * 수신자 기록 조회 실패를 도메인 예외로 옮긴다 — presentation 이 `core:network` 를 모른 채
 * 타입만으로 분기하게 하는 것이 목적이다.
 *
 * 호출부가 이 파일 하나뿐이라 **파일 스코프 `private`** 으로 둔다 (#1512). 모듈에 열어 두면
 * 「어디서든 붙일 수 있는 변환」으로 읽혀, 붙이는 자리가 늘어도 아무도 눈치채지 못한다.
 * `mapLoginFailure`(core:data) · `mapAuthoringFailure`(afternote:data) 와 같은 자리다.
 *
 * 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는 옮기지 않는다. 그 필드가 사용자
 * 노출용이라는 규정이 명세에 없다. 표시 문구는 화면이 자기 리소스로 갖는다.
 */
private fun <T> Result<T>.mapReceiverFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_DELIVERY_CONDITION_NOT_MET -> Result.failure(DeliveryNotReadyException(exception))
                else -> this
            }
        }

        else -> {
            this
        }
    }
