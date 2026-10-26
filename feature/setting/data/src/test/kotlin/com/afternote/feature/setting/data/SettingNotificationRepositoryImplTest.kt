package com.afternote.feature.setting.data

import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.UserMarketingConsentDto
import com.afternote.core.network.dto.UserPushSettingDto
import com.afternote.core.network.dto.UserUpdateMarketingConsentRequestDto
import com.afternote.core.network.dto.UserUpdatePushSettingRequestDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownHostException

class SettingNotificationRepositoryImplTest {
    @Test
    fun `getMyPushSettings - 서버 응답을 그대로 도메인으로 옮긴다`() {
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(
                    onGetMyPushSettings = {
                        dataResponse(UserPushSettingDto(timeLetter = true, mindRecord = false, afterNote = true))
                    },
                ),
            )

        val setting = runBlocking { repository.getMyPushSettings() }

        assertEquals(UserPushSetting(timeLetter = true, mindRecord = false, afterNote = true), setting)
    }

    /** 건드리지 않는 항목은 `null` 로 남겨야 서버가 나머지 토글을 덮어쓰지 않는다. */
    @Test
    fun `updateMyPushSettings - 지정한 항목만 담아 보낸다`() {
        val requests = mutableListOf<UserUpdatePushSettingRequestDto>()
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(
                    onUpdateMyPushSettings = { request ->
                        requests += request
                        dataResponse(UserPushSettingDto(timeLetter = false, mindRecord = true, afterNote = true))
                    },
                ),
            )

        val updated =
            runBlocking {
                repository.updateMyPushSettings(timeLetter = false, mindRecord = null, afterNote = null)
            }

        assertEquals(
            listOf(UserUpdatePushSettingRequestDto(timeLetter = false, mindRecord = null, afterNote = null)),
            requests,
        )
        assertEquals(UserPushSetting(timeLetter = false, mindRecord = true, afterNote = true), updated)
    }

    /**
     * 화면은 네트워크와 서버 오류 안내를 나눠 띄운다 — 분류가 이 구현에서 빠지면 두 경우가 같은
     * 문구로 합쳐지고 사용자는 재시도 여부를 판단할 근거를 잃는다.
     */
    @Test
    fun `updateMyPushSettings - 전송 실패는 네트워크 도메인 실패로 분류한다`() {
        val cause = UnknownHostException("offline")
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(onUpdateMyPushSettings = { throw cause }),
            )

        val failure =
            assertThrows(PushSettingFailure.NetworkUnavailable::class.java) {
                runBlocking { repository.updateMyPushSettings(timeLetter = null, mindRecord = false, afterNote = null) }
            }

        assertSame(cause, failure.cause)
    }

    @Test
    fun `updateMyPushSettings - 봉투 실패는 서버 도메인 실패로 분류한다`() {
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(
                    onUpdateMyPushSettings = { BaseResponse(status = 503, code = 1503) },
                ),
            )

        val failure =
            assertThrows(PushSettingFailure.ServerUnavailable::class.java) {
                runBlocking { repository.updateMyPushSettings(timeLetter = null, mindRecord = null, afterNote = false) }
            }

        assertEquals(ApiException::class.java, failure.cause?.javaClass)
    }

    /** 마케팅 동의는 푸시 토글과 달리 실패를 분류하지 않는다 — 원본 예외가 그대로 화면까지 간다. */
    @Test
    fun `updateMyMarketingConsents - 실패를 분류하지 않고 원본을 그대로 올린다`() {
        val cause = UnknownHostException("offline")
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(onUpdateMyMarketingConsents = { throw cause }),
            )

        val thrown =
            assertThrows(UnknownHostException::class.java) {
                runBlocking { repository.updateMyMarketingConsents(sms = false, email = null, push = null) }
            }

        assertSame(cause, thrown)
    }

    @Test
    fun `마케팅 동의 조회와 수정은 지정한 항목만 담아 보내고 서버 응답을 옮긴다`() {
        val requests = mutableListOf<UserUpdateMarketingConsentRequestDto>()
        val repository =
            SettingNotificationRepositoryImpl(
                SettingUserApiServiceFake(
                    onGetMyMarketingConsents = {
                        dataResponse(UserMarketingConsentDto(sms = true, email = true, push = false))
                    },
                    onUpdateMyMarketingConsents = { request ->
                        requests += request
                        dataResponse(UserMarketingConsentDto(sms = false, email = true, push = false))
                    },
                ),
            )

        val loaded = runBlocking { repository.getMyMarketingConsents() }
        val updated = runBlocking { repository.updateMyMarketingConsents(sms = false, email = null, push = null) }

        assertEquals(UserMarketingConsent(sms = true, email = true, push = false), loaded)
        assertEquals(
            listOf(UserUpdateMarketingConsentRequestDto(sms = false, email = null, push = null)),
            requests,
        )
        assertEquals(UserMarketingConsent(sms = false, email = true, push = false), updated)
    }
}
