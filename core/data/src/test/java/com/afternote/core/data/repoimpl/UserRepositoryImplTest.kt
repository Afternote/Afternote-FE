package com.afternote.core.data.repoimpl

import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    ) = UserRepositoryImpl(
        userApiService =
            FakeUserApiService(
                onDeleteAccount = {
                    calls += "deleteAccount"
                    deleteAccountResponse
                },
            ),
        authRepository =
            FakeAuthRepository(
                onClearSession = {
                    calls += "clearSession"
                    clearSessionResult
                },
            ),
    )

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

private class FakeUserApiService(
    private val onDeleteAccount: () -> BaseResponse<Unit>,
) : UserApiService {
    override suspend fun deleteAccount(): BaseResponse<Unit> = onDeleteAccount()

    override suspend fun getReceivers(): BaseResponse<List<ReceiverListDto>> = TODO("이 테스트 미사용")

    override suspend fun createReceiver(request: UserCreateReceiverRequestDto): BaseResponse<UserCreateReceiverDto> = TODO("이 테스트 미사용")

    override suspend fun getReceiverDetail(receiverId: Long): BaseResponse<ReceiverDetailDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiver(
        receiverId: Long,
        request: UserPatchReceiverRequestDto,
    ): BaseResponse<UserPatchReceiverDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        request: UserUpdateReceiverMessageRequestDto,
    ): BaseResponse<Unit> = TODO("이 테스트 미사용")

    override suspend fun getMyProfile(): BaseResponse<UserDto> = TODO("이 테스트 미사용")

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

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        request: ReceiverDeliveryConditionUpdateRequestDto,
    ): BaseResponse<ReceiverDeliveryConditionDto> = TODO("이 테스트 미사용")
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
