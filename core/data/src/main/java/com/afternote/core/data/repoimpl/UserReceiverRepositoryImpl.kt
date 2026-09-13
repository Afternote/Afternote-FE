package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.delivery.toRequestDto
import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import com.afternote.core.data.mapper.delivery.toDomain as toDeliveryConditionsDomain

/**
 * 사용자 계정에 등록된 수신자 계약 구현 (#1282).
 *
 * 이 구현만 수신자 갱신 revision 과 «로그인 구간 + collector» 에 귀속된 목록 캐시를 소유한다 —
 * 프로필·계정·푸시 설정과 상태를 공유하지 않는다.
 *
 * `@Singleton` 인 이유 — 위 revision 과 캐시는 합본 [UserRepositoryImpl] 을 거치는 경로와 [UserReceiverRepository]
 * 를 직접 주입받는 경로가 **같은 인스턴스** 로 봐야 한다(좁은 계약으로 만든 수신자가 합본 구독자의 목록도
 * 갱신한다). 스코프를 인터페이스 바인딩이 아니라 상태를 실제로 가진 이 클래스에 두면 어느 경로로 요청해도
 * 하나로 수렴한다.
 */
@Singleton
internal class UserReceiverRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val authRepository: AuthRepository,
        private val errorReporter: ErrorReporter,
    ) : UserReceiverRepository {
        private val receiverRefreshRevision = MutableStateFlow(0L)

        @OptIn(ExperimentalCoroutinesApi::class)
        override val receiverListStateFlow: Flow<ReceiverListState> =
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .flatMapLatest { loggedIn ->
                    if (!loggedIn) {
                        flowOf(ReceiverListState.SignedOut)
                    } else {
                        receiverListForAuthenticatedSession()
                    }
                }

        // 기존 소비자는 로딩 중 방출을 받지 않으며 실패 시 같은 구독의 마지막 목록을 받는다.
        override val receiverListFlow: Flow<List<Receiver>> =
            receiverListStateFlow.mapNotNull { state ->
                when (state) {
                    is ReceiverListState.Loading -> null
                    is ReceiverListState.Success -> state.receivers
                    is ReceiverListState.Failure -> state.previousReceivers.orEmpty()
                    ReceiverListState.SignedOut -> emptyList()
                }
            }

        private fun receiverListForAuthenticatedSession(): Flow<ReceiverListState> =
            flow {
                // 저장소가 아닌 로그인 구간 + collector에 귀속하여 계정 간 캐시 공유를 막는다.
                var lastKnownReceivers: List<Receiver>? = null
                receiverRefreshRevision.collect {
                    emit(ReceiverListState.Loading(lastKnownReceivers))
                    val state =
                        runCatchingCancellable { getReceivers() }
                            .fold(
                                onSuccess = { receivers ->
                                    lastKnownReceivers = receivers
                                    ReceiverListState.Success(receivers)
                                },
                                onFailure = { failure ->
                                    errorReporter.recordFailure(
                                        throwable = failure,
                                        attributes = mapOf(KEY_STAGE to STAGE_RECEIVER_LIST),
                                    )
                                    if (failure is ApiException && failure.status == UNAUTHORIZED_STATUS) {
                                        lastKnownReceivers = null
                                    }
                                    ReceiverListState.Failure(lastKnownReceivers)
                                },
                            )
                    emit(state)
                }
            }

        override fun refreshReceiverList() {
            receiverRefreshRevision.update { it + 1 }
        }

        /**
         * 비로그인 상태에서는 서버를 호출하지 않고 빈 목록을 돌려준다. 따라서 호출처는 빈 목록만으로
         * «수신인 없음» 과 «로그인 안 됨» 을 구분할 수 없다 — 구분이 필요하면 [AuthRepository.isLoggedIn] 을 함께 봐야 한다.
         */
        override suspend fun getReceivers(): List<Receiver> {
            if (!authRepository.isLoggedIn.first()) return emptyList()

            return userApiService
                .getReceivers()
                .requireData()
                .map { it.toDomain() }
        }

        override suspend fun createReceiver(
            name: String,
            relation: String,
            phone: String?,
            email: String,
            message: String?,
        ): ReceiverCreated {
            val result =
                mapReceiverRequestFailure {
                    userApiService
                        .createReceiver(
                            UserCreateReceiverRequestDto(
                                name = name,
                                relation = relation,
                                phone = phone,
                                email = email,
                                message = message,
                            ),
                        ).requireData()
                        .toDomain()
                }
            receiverRefreshRevision.update { it + 1 }
            return result
        }

        override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail =
            userApiService
                .getReceiverDetail(receiverId)
                .requireData()
                .toDomain()

        override suspend fun updateReceiver(
            receiverId: Long,
            name: String,
            phone: String,
            relation: String,
            email: String,
        ): Receiver =
            mapReceiverRequestFailure {
                userApiService
                    .updateReceiver(
                        receiverId = receiverId,
                        request =
                            UserPatchReceiverRequestDto(
                                name = name,
                                phone = phone,
                                relation = relation,
                                email = email,
                            ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun updateReceiverMessage(
            receiverId: Long,
            message: String,
        ) {
            mapReceiverRequestFailure {
                userApiService
                    .updateReceiverMessage(
                        receiverId = receiverId,
                        request = UserUpdateReceiverMessageRequestDto(message = message),
                    ).requireStatus()
            }
        }

        override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions =
            userApiService
                .getReceiverDeliveryConditions(receiverId)
                .requireData()
                .toDeliveryConditionsDomain()

        override suspend fun updateReceiverDeliveryConditions(
            receiverId: Long,
            conditions: List<DeliveryConditionItem>,
        ): ReceiverDeliveryConditions =
            userApiService
                .updateReceiverDeliveryConditions(
                    receiverId = receiverId,
                    request = ReceiverDeliveryConditionUpdateRequestDto(conditions.map { it.toRequestDto() }),
                ).requireData()
                .toDeliveryConditionsDomain()

        private companion object {
            const val UNAUTHORIZED_STATUS = 401
        }
    }

private suspend inline fun <T> mapReceiverRequestFailure(request: suspend () -> T): T =
    try {
        request()
    } catch (error: ApiException) {
        val serverMessage = error.serverMessage
        if (error.status in setOf(400, 409) && !serverMessage.isNullOrBlank()) {
            throw ReceiverRequestRejectedException(serverMessage, error)
        }
        throw error
    }

private const val KEY_STAGE = "stage"
private const val STAGE_RECEIVER_LIST = "receiver_list"
