package com.afternote.feature.setting.data

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.network.dto.SocialAccountLinkRequestDto
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.UserApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingAccountRepositoryImplTest {
    private val calls = mutableListOf<String>()
    private val errorReporter = RecordingErrorReporter()

    private fun repository(
        deleteAccountResponse: BaseResponse<Unit> = success(),
        clearSessionResult: Result<Unit> = Result.success(Unit),
        userApiService: UserApiService =
            SettingUserApiServiceFake(
                onDeleteAccount = {
                    calls += "deleteAccount"
                    deleteAccountResponse
                },
            ),
    ) = SettingAccountRepositoryImpl(
        userApiService = userApiService,
        authRepository =
            FakeAuthRepository.strict(loggedIn = true).apply {
                onClearSession = {
                    calls += "clearSession"
                    clearSessionResult
                }
            },
        errorReporter = errorReporter,
    )

    @Test
    fun `deleteAccount - 탈퇴 성공 시 로컬 세션을 정리한다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
        assertEquals(0, errorReporter.writtenFailures.size)
    }

    @Test
    fun `deleteAccount - 서버 탈퇴 실패면 세션을 유지한다`() {
        val repository = repository(deleteAccountResponse = BaseResponse(status = 500, code = 500))

        assertThrows(ApiException::class.java) {
            runBlocking { repository.deleteAccount() }
        }

        assertEquals(listOf("deleteAccount"), calls)
        assertEquals(0, errorReporter.writtenFailures.size)
    }

    /**
     * 서버 계정은 이미 지워진 뒤라 정리 실패를 예외로 올리면 화면이 "탈퇴 실패" 로 표시되고,
     * 사용자의 재시도는 없는 계정에 대해 다시 실패한다. 삼키는 것이 계약이다.
     */
    @Test
    fun `deleteAccount - 세션 정리가 실패해도 탈퇴는 성공으로 끝난다`() {
        val failure = IllegalStateException("datastore 쓰기 실패")
        val repository = repository(clearSessionResult = Result.failure(failure))

        runBlocking { repository.deleteAccount() }

        assertEquals(listOf("deleteAccount", "clearSession"), calls)
        val (reported, attributes) = errorReporter.writtenFailures.single()
        assertEquals(IllegalStateException::class.java.name, reported.message)
        assertEquals(
            mapOf(
                "account_stage" to "delete_session_cleanup",
                "error_type" to IllegalStateException::class.java.name,
            ),
            attributes,
        )
    }

    /** 정리가 DELETE 앞에 오면 요청이 토큰 없이 나가므로, 순서 자체가 계약이다. */
    @Test
    fun `deleteAccount - 세션 정리는 서버 호출 뒤에 온다`() {
        val repository = repository()

        runBlocking { repository.deleteAccount() }

        assertEquals(0, calls.indexOf("deleteAccount"))
        assertEquals(1, calls.indexOf("clearSession"))
    }

    /** 연결 계정 응답은 DTO 필드를 한 칸도 섞지 않고 도메인으로 옮긴다. */
    @Test
    fun `getConnectedAccounts - 서버 응답을 그대로 도메인으로 옮긴다`() {
        val repository =
            repository(
                userApiService =
                    SettingUserApiServiceFake(
                        onGetConnectedAccounts = { dataResponse(CONNECTED_ACCOUNT_DTO) },
                    ),
            )

        val accounts = runBlocking { repository.getConnectedAccounts() }

        assertEquals(EXPECTED_CONNECTED_ACCOUNT, accounts)
    }

    /** provider 는 경로, accessToken 은 본문 — 자리가 바뀌면 서버가 연결 대상을 잘못 잡는다. */
    @Test
    fun `linkConnectedAccount - provider 와 accessToken 을 각자 자리로 보낸다`() {
        val requests = mutableListOf<Pair<String, SocialAccountLinkRequestDto>>()
        val repository =
            repository(
                userApiService =
                    SettingUserApiServiceFake(
                        onLinkConnectedAccount = { provider, request ->
                            requests += provider to request
                            dataResponse(CONNECTED_ACCOUNT_DTO)
                        },
                    ),
            )

        val accounts = runBlocking { repository.linkConnectedAccount(provider = "google", accessToken = "google-token") }

        assertEquals(listOf("google" to SocialAccountLinkRequestDto(accessToken = "google-token")), requests)
        assertEquals(EXPECTED_CONNECTED_ACCOUNT, accounts)
    }

    @Test
    fun `unlinkConnectedAccount - 해제 뒤 서버가 준 최신 연결 상태를 돌려준다`() {
        val providers = mutableListOf<String>()
        val repository =
            repository(
                userApiService =
                    SettingUserApiServiceFake(
                        onUnlinkConnectedAccount = { provider ->
                            providers += provider
                            dataResponse(CONNECTED_ACCOUNT_DTO.copy(google = false, googleEmail = null))
                        },
                    ),
            )

        val accounts = runBlocking { repository.unlinkConnectedAccount("google") }

        assertEquals(listOf("google"), providers)
        assertEquals(EXPECTED_CONNECTED_ACCOUNT.copy(google = false, googleEmail = null), accounts)
    }

    private companion object {
        val CONNECTED_ACCOUNT_DTO =
            UserConnectedAccountDto(
                local = true,
                google = true,
                naver = false,
                kakao = false,
                apple = false,
                localEmail = "local@afternote.local",
                googleEmail = "google@afternote.local",
                naverEmail = null,
                kakaoEmail = null,
                appleEmail = null,
            )

        val EXPECTED_CONNECTED_ACCOUNT =
            UserConnectedAccount(
                local = true,
                google = true,
                naver = false,
                kakao = false,
                apple = false,
                localEmail = "local@afternote.local",
                googleEmail = "google@afternote.local",
                naverEmail = null,
                kakaoEmail = null,
                appleEmail = null,
            )
    }
}

private fun success() = BaseResponse<Unit>(status = 200, code = 200)
