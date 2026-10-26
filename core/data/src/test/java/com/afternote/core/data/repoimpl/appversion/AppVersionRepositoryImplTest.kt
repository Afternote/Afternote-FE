package com.afternote.core.data.repoimpl.appversion

import com.afternote.core.network.dto.AppPlatformDto
import com.afternote.core.network.dto.AppVersionDto
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.AppVersionApiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 강제 업데이트 조회의 경계 계약 회귀 가드 (#1539).
 *
 * 관문이 fail-open 이려면 이 경계가 **예외를 밖으로 내보내지 않아야** 한다. 전송 실패든
 * 봉투 실패든 전부 `Result.failure` 로 접어 호출자가 «못 물어봤다» 하나로 다룰 수 있게 한다.
 */
class AppVersionRepositoryImplTest {
    @Test
    fun `서버 응답을 도메인 모델로 옮긴다`() {
        val service =
            FakeAppVersionApiService { _, _ ->
                envelope(AppVersionDto(updateRequired = true, latestVersionCode = 10_001, storeUrl = STORE_URL))
            }

        val result = runBlocking { AppVersionRepositoryImpl(service).checkAndroidVersion(10_000) }

        val check = result.getOrThrow()
        assertTrue(check.updateRequired)
        assertEquals(10_001, check.latestVersionCode)
        assertEquals(STORE_URL, check.storeUrl)
    }

    @Test
    fun `플랫폼은 ANDROID 로 고정하고 versionCode 는 그대로 보낸다`() {
        var seen: Pair<AppPlatformDto, Int>? = null
        val service =
            FakeAppVersionApiService { platform, versionCode ->
                seen = platform to versionCode
                envelope(AppVersionDto(updateRequired = false, latestVersionCode = 10_001, storeUrl = null))
            }

        runBlocking { AppVersionRepositoryImpl(service).checkAndroidVersion(10_000) }

        assertEquals(AppPlatformDto.ANDROID to 10_000, seen)
    }

    @Test
    fun `업데이트가 필요 없으면 스토어 주소는 null 이다`() {
        val service =
            FakeAppVersionApiService { _, _ ->
                envelope(AppVersionDto(updateRequired = false, latestVersionCode = 10_001, storeUrl = null))
            }

        val result = runBlocking { AppVersionRepositoryImpl(service).checkAndroidVersion(10_001) }

        assertNull(result.getOrThrow().storeUrl)
    }

    @Test
    fun `전송 실패는 예외를 던지지 않고 실패 Result 가 된다`() {
        val service = FakeAppVersionApiService { _, _ -> throw IOException("서버 무응답") }

        val result = runBlocking { AppVersionRepositoryImpl(service).checkAndroidVersion(10_000) }

        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `봉투가 실패를 말하면 실패 Result 가 된다`() {
        val service =
            FakeAppVersionApiService { _, _ ->
                BaseResponse(status = 500, code = 5000, message = "앱 버전 릴리스가 등록되지 않았습니다.", data = null)
            }

        val result = runBlocking { AppVersionRepositoryImpl(service).checkAndroidVersion(10_000) }

        assertTrue(result.exceptionOrNull() is ApiException)
    }

    private fun envelope(dto: AppVersionDto) = BaseResponse(status = 200, code = 200, message = "성공", data = dto)

    private companion object {
        const val STORE_URL = "https://play.google.com/store/apps/details?id=com.afternote.afternote_fe"
    }
}

private class FakeAppVersionApiService(
    private val onCheck: (AppPlatformDto, Int) -> BaseResponse<AppVersionDto>,
) : AppVersionApiService {
    override suspend fun checkVersion(
        platform: AppPlatformDto,
        versionCode: Int,
    ): BaseResponse<AppVersionDto> = onCheck(platform, versionCode)
}
