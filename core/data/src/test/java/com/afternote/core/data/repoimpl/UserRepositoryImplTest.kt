package com.afternote.core.data.repoimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.data.session.UserReceiverCache
import com.afternote.core.data.session.UserSessionBoundary
import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.session.UserSessionChangedException
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.DeliveryConditionDto
import com.afternote.core.network.dto.DeliveryConditionRequestDto
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.dto.ReceiverListDto
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.dto.UserCreateReceiverDto
import com.afternote.core.network.dto.UserCreateReceiverRequestDto
import com.afternote.core.network.dto.UserDto
import com.afternote.core.network.dto.UserPatchReceiverDto
import com.afternote.core.network.dto.UserPatchReceiverRequestDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.dto.UserUpdateReceiverMessageRequestDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [UserRepositoryImpl] 의 회원 탈퇴 세션 정리 계약 회귀 가드 (#586).
 *
 * 계약 — 서버 탈퇴가 성공한 **뒤에만** 로컬 세션을 정리한다. 정리를 빠뜨리면 재시작 시 삭제된
 * 계정의 토큰으로 홈이 뜨고, 앞당기면 DELETE 요청 자체가 토큰을 잃는다.
 */
class UserRepositoryImplTest {
    private val calls = mutableListOf<String>()

    private fun repository(
        deleteAccountResponse: BaseResponse<Unit> = success(),
        clearSessionResult: Result<Unit> = Result.success(Unit),
        sessionBoundary: UserSessionBoundary = UserSessionBoundary(),
        receiverCache: UserReceiverCache = UserReceiverCache(sessionBoundary),
        userProfileDataSource: UserProfileDataSource = UserProfileDataSource(InMemoryPreferencesDataStore()),
        onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>> = {
            error("getReceivers 는 이 시나리오에서 호출되면 안 됨")
        },
        onCreateReceiver: suspend () -> BaseResponse<UserCreateReceiverDto> = {
            error("createReceiver 는 이 시나리오에서 호출되면 안 됨")
        },
        onGetReceiverDetail: suspend (Long) -> BaseResponse<ReceiverDetailDto> = {
            error("getReceiverDetail 은 이 시나리오에서 호출되면 안 됨")
        },
        onUpdateReceiver: suspend (Long, UserPatchReceiverRequestDto) -> BaseResponse<UserPatchReceiverDto> = { _, _ ->
            error("updateReceiver 는 이 시나리오에서 호출되면 안 됨")
        },
        onGetMyProfile: suspend () -> BaseResponse<UserDto> = {
            error("getMyProfile 은 이 시나리오에서 호출되면 안 됨")
        },
    ) = UserRepositoryImpl(
        userApiService =
            FakeUserApiService(
                onDeleteAccount = {
                    calls += "deleteAccount"
                    deleteAccountResponse
                },
                onGetReceivers = onGetReceivers,
                onCreateReceiver = onCreateReceiver,
                onGetReceiverDetail = onGetReceiverDetail,
                onUpdateReceiver = onUpdateReceiver,
                onGetMyProfile = onGetMyProfile,
            ),
        authRepository =
            FakeAuthRepository(
                onClearSession = {
                    calls += "clearSession"
                    clearSessionResult
                },
            ),
        receiverCache = receiverCache,
        sessionBoundary = sessionBoundary,
        userProfileDataSource = userProfileDataSource,
    )

    @Test
    fun `getReceivers - 세션 초기화 뒤 빈 계정은 다시 조회하고 빈 목록을 캐시한다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            var response = listOf(receiverDto(name = "계정 A 수신자"))
            var requestCount = 0
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onGetReceivers = {
                        requestCount += 1
                        success(response)
                    },
                )

            assertEquals("계정 A 수신자", repository.getReceivers().single().name)
            assertEquals("계정 A 수신자", repository.getReceivers().single().name)
            assertEquals(1, requestCount)

            sessionBoundary.transition(activateAfter = true) {
                receiverCache.clear()
            }
            response = emptyList()

            assertTrue(repository.getReceivers().isEmpty())
            assertTrue(
                repository.receiverListFlow
                    .take(1)
                    .toList()
                    .single()
                    .isEmpty(),
            )
            assertEquals(2, requestCount)
        }

    @Test
    fun `receiverListFlow - 세션 초기화 시 기존 구독자에게 빈 목록을 방출한다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val firstEmission = CompletableDeferred<Unit>()
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onGetReceivers = { success(listOf(receiverDto(name = "계정 A 수신자"))) },
                )
            val emissions =
                async {
                    repository.receiverListFlow
                        .onEach { receivers ->
                            if (receivers.isNotEmpty()) firstEmission.complete(Unit)
                        }.take(2)
                        .toList()
                }

            firstEmission.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
            }

            assertEquals(
                listOf(listOf("계정 A 수신자"), emptyList<String>()),
                emissions.await().map { receivers -> receivers.map { it.name } },
            )
        }

    @Test
    fun `getReceivers - 세션 전환 뒤 완료된 이전 응답은 캐시하거나 반환하지 않는다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val requestStarted = CompletableDeferred<Unit>()
            val releaseResponse = CompletableDeferred<Unit>()
            var requestCount = 0
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onGetReceivers = {
                        requestCount += 1
                        if (requestCount == 1) {
                            requestStarted.complete(Unit)
                            releaseResponse.await()
                            success(listOf(receiverDto(name = "계정 A 수신자")))
                        } else {
                            success(emptyList())
                        }
                    },
                )
            val previousSessionRequest = async { repository.getReceivers() }

            requestStarted.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
            }
            releaseResponse.complete(Unit)

            assertTrue(previousSessionRequest.await().isEmpty())
            assertNull(receiverCache.receiversFor(sessionBoundary.current))
            sessionBoundary.transition(activateAfter = true) {
                receiverCache.clear()
            }
            assertTrue(repository.getReceivers().isEmpty())
            assertEquals(2, requestCount)
        }

    @Test
    fun `createReceiver - 세션 전환 뒤 완료된 목록 갱신은 이전 응답을 캐시하지 않는다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val refreshStarted = CompletableDeferred<Unit>()
            val releaseRefresh = CompletableDeferred<Unit>()
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onCreateReceiver = {
                        success(
                            UserCreateReceiverDto(
                                receiverId = 1L,
                                authCode = "AUTH-CODE-A",
                            ),
                        )
                    },
                    onGetReceivers = {
                        refreshStarted.complete(Unit)
                        releaseRefresh.await()
                        success(listOf(receiverDto(name = "계정 A 수신자")))
                    },
                )
            val createRequest =
                async {
                    runCatching {
                        repository.createReceiver(
                            name = "계정 A 수신자",
                            relation = "친구",
                            phone = null,
                            email = null,
                            message = null,
                        )
                    }
                }

            refreshStarted.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
            }
            releaseRefresh.complete(Unit)

            val failure = createRequest.await().exceptionOrNull()
            assertTrue(failure is UserSessionChangedException)
            assertNull(receiverCache.receiversFor(sessionBoundary.current))
        }

    @Test
    fun `getReceiverDetail - 세션 전환 뒤 완료된 이전 상세 응답은 반환하지 않는다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val requestStarted = CompletableDeferred<Unit>()
            val releaseResponse = CompletableDeferred<Unit>()
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onGetReceiverDetail = {
                        requestStarted.complete(Unit)
                        releaseResponse.await()
                        success(receiverDetailDto())
                    },
                )
            val previousSessionRequest = async { runCatching { repository.getReceiverDetail(receiverId = 1L) } }

            requestStarted.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
            }
            releaseResponse.complete(Unit)

            val result = previousSessionRequest.await()
            assertTrue(result.exceptionOrNull() is UserSessionChangedException)
            assertNull(result.getOrNull())
        }

    @Test
    fun `updateReceiver - 세션 전환 뒤 완료된 이전 수정 응답은 반환하지 않는다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val requestStarted = CompletableDeferred<Unit>()
            val releaseResponse = CompletableDeferred<Unit>()
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    onUpdateReceiver = { _, _ ->
                        requestStarted.complete(Unit)
                        releaseResponse.await()
                        success(patchedReceiverDto())
                    },
                )
            val previousSessionRequest =
                async {
                    runCatching {
                        repository.updateReceiver(
                            receiverId = 1L,
                            name = "계정 A 수신자",
                            phone = "01012345678",
                            relation = "친구",
                            email = "receiver-a@example.com",
                        )
                    }
                }

            requestStarted.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
            }
            releaseResponse.complete(Unit)

            val result = previousSessionRequest.await()
            assertTrue(result.exceptionOrNull() is UserSessionChangedException)
            assertNull(result.getOrNull())
        }

    @Test
    fun `getMyProfile - 성공한 사용자 이름을 로컬 캐시에 저장한다`() =
        runBlocking {
            val profileDataSource = UserProfileDataSource(InMemoryPreferencesDataStore())
            val repository =
                repository(
                    userProfileDataSource = profileDataSource,
                    onGetMyProfile = {
                        success(
                            UserDto(
                                name = "새 사용자",
                                email = "new@example.com",
                            ),
                        )
                    },
                )

            repository.getMyProfile()

            assertEquals("새 사용자", profileDataSource.getCachedUserName())
        }

    @Test
    fun `getMyProfile - 세션 전환 뒤 완료된 이전 프로필은 저장하거나 반환하지 않는다`() =
        runBlocking {
            val sessionBoundary = UserSessionBoundary()
            val receiverCache = UserReceiverCache(sessionBoundary)
            val profileDataSource = UserProfileDataSource(InMemoryPreferencesDataStore())
            val requestStarted = CompletableDeferred<Unit>()
            val releaseResponse = CompletableDeferred<Unit>()
            val repository =
                repository(
                    sessionBoundary = sessionBoundary,
                    receiverCache = receiverCache,
                    userProfileDataSource = profileDataSource,
                    onGetMyProfile = {
                        requestStarted.complete(Unit)
                        releaseResponse.await()
                        success(
                            UserDto(
                                name = "계정 A",
                                email = "account-a@example.com",
                            ),
                        )
                    },
                )
            val previousSessionRequest = async { runCatching { repository.getMyProfile() } }

            requestStarted.await()
            sessionBoundary.transition(activateAfter = false) {
                receiverCache.clear()
                profileDataSource.clear()
            }
            releaseResponse.complete(Unit)

            val failure = previousSessionRequest.await().exceptionOrNull()
            assertTrue(failure is UserSessionChangedException)
            assertNull(profileDataSource.getCachedUserName())
        }

    @Test
    fun `deleteAccount - 탈퇴 성공 시 로컬 세션을 정리한다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
    }

    @Test
    fun `deleteAccount - 서버 탈퇴 실패면 세션을 유지한다`() {
        val repository = repository(deleteAccountResponse = BaseResponse(status = 500, code = 500))

        assertThrows(ApiException::class.java) {
            runBlocking { repository.deleteAccount() }
        }

        assertEquals(listOf("deleteAccount"), calls)
    }

    /**
     * 서버 계정은 이미 지워진 뒤라 정리 실패를 예외로 올리면 화면이 "탈퇴 실패" 로 표시되고,
     * 사용자의 재시도는 없는 계정에 대해 다시 실패한다. 삼키는 것이 계약이다.
     */
    @Test
    fun `deleteAccount - 세션 정리가 실패해도 탈퇴는 성공으로 끝난다`() {
        val repository = repository(clearSessionResult = Result.failure(IllegalStateException("datastore 쓰기 실패")))

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
    }

    /** 정리가 DELETE 앞에 오면 요청이 토큰 없이 나가므로, 순서 자체가 계약이다. */
    @Test
    fun `deleteAccount - 세션 정리는 서버 호출 뒤에 온다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(0, calls.indexOf("deleteAccount"))
        assertEquals(1, calls.indexOf("clearSession"))
    }
}

private fun success() = BaseResponse<Unit>(status = 200, code = 200)

private fun <T> success(data: T) = BaseResponse(status = 200, code = 200, data = data)

private fun receiverDto(name: String) =
    ReceiverListDto(
        receiverId = 1L,
        name = name,
        relation = "친구",
        authCode = "AUTH-CODE",
    )

private fun receiverDetailDto() =
    ReceiverDetailDto(
        receiverId = 1L,
        name = "계정 A 수신자",
        relation = "친구",
        phone = "01012345678",
        email = "receiver-a@example.com",
        dailyQuestionCount = 1,
        timeLetterCount = 2,
        afterNoteCount = 3,
        message = "계정 A 메시지",
        authCode = "AUTH-CODE-A",
    )

private fun patchedReceiverDto() =
    UserPatchReceiverDto(
        receiverId = 1L,
        name = "계정 A 수신자",
        phone = "01012345678",
        relation = "친구",
        email = "receiver-a@example.com",
    )

private class FakeUserApiService(
    private val onDeleteAccount: () -> BaseResponse<Unit>,
    private val onGetReceivers: suspend () -> BaseResponse<List<ReceiverListDto>>,
    private val onCreateReceiver: suspend () -> BaseResponse<UserCreateReceiverDto>,
    private val onGetReceiverDetail: suspend (Long) -> BaseResponse<ReceiverDetailDto>,
    private val onUpdateReceiver: suspend (Long, UserPatchReceiverRequestDto) -> BaseResponse<UserPatchReceiverDto>,
    private val onGetMyProfile: suspend () -> BaseResponse<UserDto>,
) : UserApiService {
    override suspend fun deleteAccount(): BaseResponse<Unit> = onDeleteAccount()

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = onGetReceivers()

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> = onCreateReceiver()

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = onGetReceiverDetail(receiverId)

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = onUpdateReceiver(receiverId, request)

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = onGetMyProfile()

    override suspend fun updateMyProfile(request: UserUpdateProfileRequestDto): BaseResponse<UserDto> = TODO("이 테스트 미사용")

    override suspend fun logActivity(): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyPushSettings(): BaseResponse<UserPushSettingDto> = TODO("이 테스트 미사용")

    override suspend fun updateMyPushSettings(request: UserUpdatePushSettingRequestDto): BaseResponse<UserPushSettingDto> =
        TODO("이 테스트 미사용")

    override suspend fun getConnectedAccounts(): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun linkConnectedAccount(
        provider: String,
        request: SocialAccountLinkRequestDto,
    ): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun unlinkConnectedAccount(provider: String): BaseResponse<UserConnectedAccountDto> = TODO("이 테스트 미사용")

    override suspend fun getDeliveryCondition(): BaseResponse<DeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateDeliveryCondition(request: DeliveryConditionRequestDto): BaseResponse<DeliveryConditionDto> =
        TODO("이 테스트 미사용")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(state.value)
        state.value = transformed
        return transformed
    }
}

private class FakeAuthRepository(
    private val onClearSession: () -> Result<Unit>,
) : AuthRepository {
    override val isLoggedIn: Flow<Boolean> = flowOf(true)

    override suspend fun clearSession(): Result<Unit> = onClearSession()

    override suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = TODO("이 테스트 미사용")

    override suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    ): Result<Unit> = TODO("이 테스트 미사용")

    override suspend fun getAccessToken(): Result<String?> = TODO("이 테스트 미사용")

    override suspend fun getRefreshToken(): Result<String?> = TODO("이 테스트 미사용")

    override suspend fun defaultLogin(
        email: String,
        password: String,
    ): Result<Session.DefaultSession> = TODO("이 테스트 미사용")

    override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> = TODO("이 테스트 미사용")

    override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> = TODO("이 테스트 미사용")

    override suspend fun rotateToken(): Result<TokenBundle> = TODO("이 테스트 미사용")

    override suspend fun logout(): Result<Unit> = TODO("이 테스트 미사용")
}
