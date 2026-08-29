package com.afternote.core.data.repoimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.data.mapper.delivery.toRequestDto
import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
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
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import com.afternote.core.data.mapper.delivery.toDomain as toDeliveryConditionsDomain

// `app` 의 androidTest(SettingCompletionAndroidTest)가 Hilt 를 우회해 이 구현을 직접 조립하므로
// 형제 구현체들과 달리 `internal` 로 닫지 못한다. 닫으려면 그 테스트의 페이크 기반 개조가 선행돼야 한다 (#930).
class UserRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val authRepository: AuthRepository,
        private val errorReporter: ErrorReporter,
    ) : UserRepository {
        private val receiverRefreshRevision = MutableStateFlow(0L)

        // 조회 실패를 예외로 흘리면 구독 중인 화면이 미처리 예외로 죽는다. 일반적인 일시 실패는 같은
        // 로그인 구간에서 이 collector가 마지막으로 성공한 목록으로 낮춘다. 캐시를 flow 안에 두는 이유는
        // 저장소 인스턴스보다 수명이 짧은 «로그인 구간 + collector»에 귀속해 계정 사이에 섞이지 않게 하기 위함이다.
        @OptIn(ExperimentalCoroutinesApi::class)
        override val receiverListFlow: Flow<List<Receiver>> =
            authRepository.isLoggedIn
                .distinctUntilChanged()
                .flatMapLatest { loggedIn ->
                    if (!loggedIn) {
                        flowOf(emptyList())
                    } else {
                        receiverListForAuthenticatedSession()
                    }
                }

        private fun receiverListForAuthenticatedSession(): Flow<List<Receiver>> =
            flow {
                var lastKnownReceivers = emptyList<Receiver>()
                receiverRefreshRevision.collect {
                    val receivers =
                        runCatchingCancellable { getReceivers() }
                            .onFailure {
                                // 이 flow 가 하는 일이 «예외를 삼켜 화면을 살리는 것» 이라, 삼킨 뒤의
                                // 기록이 이 실패 경로의 유일한 신호다. logcat 은 실기에서 회수되지 않으므로
                                // 크래시 리포팅 창구로 남긴다. 취소 제외·문구 redaction 은 리포터 정책이 담당한다.
                                errorReporter.recordFailure(
                                    throwable = it,
                                    attributes = mapOf(KEY_STAGE to STAGE_RECEIVER_LIST),
                                )
                            }.fold(
                                onSuccess = { it },
                                onFailure = { failure ->
                                    if (failure is ApiException && failure.status == UNAUTHORIZED_STATUS) {
                                        emptyList()
                                    } else {
                                        lastKnownReceivers
                                    }
                                },
                            )
                    lastKnownReceivers = receivers
                    emit(receivers)
                }
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
            email: String?,
            message: String?,
        ): ReceiverCreated {
            val result =
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

        override suspend fun updateReceiverMessage(
            receiverId: Long,
            message: String,
        ) {
            userApiService
                .updateReceiverMessage(
                    receiverId = receiverId,
                    request = UserUpdateReceiverMessageRequestDto(message = message),
                ).requireStatus()
        }

        override suspend fun getMyProfile(): User =
            userApiService
                .getMyProfile()
                .requireData()
                .toDomain()

        override suspend fun updateMyProfile(
            name: String?,
            phone: String?,
            profileImageUrl: String?,
        ): User =
            userApiService
                .updateMyProfile(
                    UserUpdateProfileRequestDto(
                        name = name,
                        phone = phone,
                        profileImageUrl = profileImageUrl,
                    ),
                ).requireData()
                .toDomain()

        /**
         * 탈퇴 성공 후 로컬 세션도 정리한다 (#586) — 서버가 계정을 지워도 토큰이 남으면 재시작 시
         * 죽은 토큰으로 홈이 뜨고 인증 요청이 연달아 401 로 실패한다(2026-07-28 에뮬 실측).
         *
         * 정리를 서버 호출 **뒤**에 두는 건 `AuthRepositoryImpl.logout()` 과 같은 이유 — DELETE 요청도
         * `AuthInterceptor` 를 지나므로 그 시점엔 토큰이 살아 있어야 한다.
         *
         * `clearSession()` 의 실패는 삼킨다. 서버 계정은 이미 지워졌으므로 여기서 예외를 올리면
         * 화면이 "탈퇴 실패" 로 표시돼 사용자가 재시도하고, 그 재시도는 없는 계정에 대해 실패한다.
         * 대신 [ErrorReporter] 에 비식별 단계만 붙여 남긴다 — 조용히 넘기면 로컬 토큰이 남은 채로
         * 이 버그가 재발해도 탐지되지 않는다. 예외 원문 제거는 리포터 공통 정책이 담당한다.
         */
        override suspend fun deleteAccount() {
            userApiService
                .deleteAccount()
                .requireStatus()
            authRepository.clearSession().onFailure {
                errorReporter.recordFailure(
                    throwable = it,
                    attributes = mapOf(KEY_ACCOUNT_STAGE to ACCOUNT_STAGE_DELETE_SESSION_CLEANUP),
                )
            }
        }

        override suspend fun logActivity() {
            userApiService
                .logActivity()
                .requireStatus()
        }

        override suspend fun getMyPushSettings(): UserPushSetting =
            userApiService
                .getMyPushSettings()
                .requireData()
                .toDomain()

        override suspend fun updateMyPushSettings(
            timeLetter: Boolean?,
            mindRecord: Boolean?,
            afterNote: Boolean?,
        ): UserPushSetting =
            userApiService
                .updateMyPushSettings(
                    UserUpdatePushSettingRequestDto(
                        timeLetter = timeLetter,
                        mindRecord = mindRecord,
                        afterNote = afterNote,
                    ),
                ).requireData()
                .toDomain()

        override suspend fun getConnectedAccounts(): UserConnectedAccount =
            userApiService
                .getConnectedAccounts()
                .requireData()
                .toDomain()

        override suspend fun linkConnectedAccount(
            provider: String,
            accessToken: String,
        ): UserConnectedAccount =
            userApiService
                .linkConnectedAccount(
                    provider = provider,
                    request = SocialAccountLinkRequestDto(accessToken = accessToken),
                ).requireData()
                .toDomain()

        override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount =
            userApiService
                .unlinkConnectedAccount(provider)
                .requireData()
                .toDomain()

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

private const val KEY_ACCOUNT_STAGE = "account_stage"
private const val ACCOUNT_STAGE_DELETE_SESSION_CLEANUP = "delete_session_cleanup"
private const val KEY_STAGE = "stage"
private const val STAGE_RECEIVER_LIST = "receiver_list"
