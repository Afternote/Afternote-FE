package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.delivery.toRequestDto
import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.error.ReceiverRequestRejectedException
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.repository.UserReceiverRepository
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
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
        // 세션 경계의 정본. 캐시 수명을 이 식별자에 건다(아래 receiverListFlow 주석) (#2135).
        private val tokenDataSource: TokenDataSource,
        private val errorReporter: ErrorReporter,
    ) : UserReceiverRepository {
        private val receiverRefreshRevision = MutableStateFlow(0L)

        // 조회 실패를 예외로 흘리면 구독 중인 화면이 미처리 예외로 죽는다. 일반적인 일시 실패는 같은
        // 로그인 구간에서 이 collector가 마지막으로 성공한 목록을 실은 Failure 로 낸다. 캐시를 flow 안에 두는 이유는
        // 저장소 인스턴스보다 수명이 짧은 «로그인 구간 + collector»에 귀속해 계정 사이에 섞이지 않게 하기 위함이다.
        //
        // 그 로그인 구간을 [TokenDataSource.sessionId] 로 가른다. 로그인 여부 Boolean 으로 가르면
        // 로그아웃과 새 로그인이 한 구간에 겹칠 때 수집자에게 true → true 로 뭉개져 이 flow 가 재시작되지
        // 않고, 캐시가 계정 경계를 넘는다 (#2135). 세션 식별자는 로그인마다 새 값이라 중간 로그아웃 방출이
        // 생략돼도 값이 달라지고, 같은 세션의 토큰 회전에는 값이 그대로여서 캐시가 유지된다.
        //
        // 이전 세션 추적은 수집자마다 새로 만든다. 이 저장소는 @Singleton 이라 필드에 두면 한 화면의 세션
        // 전환 판정을 다른 화면이 먹는다.
        //
        // 목록 전용 [receiverListFlow] 도 이 Flow 에서 파생한다 (#2045). 세션 분기와 늦은 결과 폐기를 한 곳에만
        // 둬야 두 공개 Flow 의 격리 규칙이 갈라지지 않는다.
        @OptIn(ExperimentalCoroutinesApi::class)
        override val receiverListStateFlow: Flow<ReceiverListState> =
            flow {
                var previousSessionId: String? = null
                emitAll(
                    tokenDataSource.sessionId
                        .distinctUntilChanged()
                        .flatMapLatest { sessionId ->
                            val previous = previousSessionId
                            previousSessionId = sessionId
                            when {
                                sessionId == null -> flowOf(ReceiverListState.SignedOut)

                                // 첫 구독과 로그아웃 뒤 로그인. 이 수집자가 들고 있는 값이 이전 계정 목록이
                                // 아니므로 곧바로 조회한다. 여기서까지 빈 목록을 먼저 내면 첫 구독을
                                // Flow.first() 로 받는 호출처가 요청 전에 끊긴다.
                                previous == null -> receiverListForSession(sessionId)

                                // 실제 세션 교체. 하류 stateIn 의 initialValue 는 내부 flow 가 교체돼도
                                // 되돌아가지 않으므로, 새 세션 조회가 끝날 때까지 이전 계정 목록이 수집자의
                                // 현재 값으로 남는다. 계약이 "새 세션에는 빈 목록" 이라 조회를 기다리지 않고
                                // 먼저 비운다. 저장소에서 합쳐져 사라진 중간 로그아웃 방출을 되살리는 셈이다.
                                else -> receiverListForSession(sessionId).onStart { emit(ReceiverListState.SignedOut) }
                            }
                        },
                )
            }

        // 기존 목록 전용 구독자의 방출 순서를 그대로 지킨다. 로딩은 방출을 만들지 않고, 실패는 이 구독이 같은
        // 세션에서 마지막으로 성공한 목록(없거나 401 로 버렸으면 빈 목록)으로 낮춘다.
        override val receiverListFlow: Flow<List<Receiver>> =
            receiverListStateFlow.mapNotNull { state ->
                when (state) {
                    is ReceiverListState.Loading -> null
                    is ReceiverListState.Success -> state.receivers
                    is ReceiverListState.Failure -> state.previousReceivers.orEmpty()
                    ReceiverListState.SignedOut -> emptyList()
                }
            }

        override fun refreshReceiverList() {
            receiverRefreshRevision.update { it + 1 }
        }

        private fun receiverListForSession(sessionId: String): Flow<ReceiverListState> =
            flow {
                var lastKnownReceivers: List<Receiver>? = null
                receiverRefreshRevision.collect {
                    // 로딩에는 이 구독이 이 세션에서 이미 낸 성공 목록만 싣는다. 수집자가 이미 받은 값 밖의 것이
                    // 새지 않으므로 아래의 세션 재확인은 여기에 걸지 않는다.
                    emit(ReceiverListState.Loading(lastKnownReceivers))
                    val outcome =
                        runCatchingCancellable { getReceivers() }
                            .onFailure {
                                // 이 flow 가 하는 일이 «예외를 삼켜 화면을 살리는 것» 이라, 삼킨 뒤의
                                // 기록이 이 실패 경로의 유일한 신호다. logcat 은 실기에서 회수되지 않으므로
                                // 크래시 리포팅 창구로 남긴다. 취소 제외·문구 redaction 은 리포터 정책이 담당한다.
                                errorReporter.recordFailure(
                                    throwable = it,
                                    attributes = mapOf(KEY_STAGE to STAGE_RECEIVER_LIST),
                                )
                            }

                    // 응답이 돌아온 지금 저장된 세션이 이 구독의 세션과 다르면 결과는 이전 계정 것이다.
                    // 세션 전환이 저장소에 커밋되고도 상위 sessionId 관측이 새 식별자를 아직 전달하지 못한
                    // 구간에는 이 flow 가 취소되지 않은 채 살아 있고, 그때 끝난 성공도 실패 폴백도 이전 계정
                    // 목록을 새 계정으로 흘려보냈다 (#2135). 재조회에서 세션이 다르면 이 결과를 반영하지
                    // 않는다. 이 비교와 방출이 원자적인 것은 아니며, 이미 내보낸 값을 되돌리지는 못한다.
                    if (tokenDataSource.sessionId.first() != sessionId) return@collect

                    val state =
                        outcome.fold(
                            onSuccess = { receivers ->
                                lastKnownReceivers = receivers
                                ReceiverListState.Success(receivers)
                            },
                            onFailure = { failure ->
                                if (failure is ApiException && failure.status == UNAUTHORIZED_STATUS) {
                                    lastKnownReceivers = null
                                }
                                ReceiverListState.Failure(lastKnownReceivers)
                            },
                        )
                    emit(state)
                }
            }

        /**
         * 비로그인 상태에서는 서버를 호출하지 않고 빈 목록을 돌려준다. 따라서 호출처는 빈 목록만으로
         * «수신인 없음» 과 «로그인 안 됨» 을 구분할 수 없다 — 구분이 필요하면
         * `AuthRepository.isLoggedIn` 을 함께 봐야 한다.
         */
        override suspend fun getReceivers(): List<Receiver> {
            if (tokenDataSource.sessionId.first() == null) return emptyList()

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

        /**
         * 등록과 마찬가지로 성공한 뒤에만 목록 revision 을 올린다 (#2127). 이름·관계가 목록에 그대로
         * 실리므로, 올리지 않으면 수정하고 목록으로 돌아온 구독자가 옛 값을 계속 보여준다.
         */
        override suspend fun updateReceiver(
            receiverId: Long,
            name: String,
            phone: String,
            relation: String,
            email: String,
        ): Receiver {
            val updated =
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
            receiverRefreshRevision.update { it + 1 }
            return updated
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
            throw ReceiverRequestRejectedException(error)
        }
        throw error
    }

private const val KEY_STAGE = "stage"
private const val STAGE_RECEIVER_LIST = "receiver_list"
