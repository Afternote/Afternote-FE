package com.afternote.core.network.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** `GET /app/version` 응답의 강제 업데이트 판정 필드 회귀 가드 (#423). */
class AppVersionContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `최신 버전 - updateRequired false 와 명시적 null storeUrl 디코드`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"updateRequired":false,"latestVersionCode":10001,"storeUrl":null}}"""

        val data = json.decodeFromString<BaseResponse<AppVersionDto>>(payload).requireData()

        assertFalse(data.updateRequired)
        assertEquals(10001, data.latestVersionCode)
        assertNull(data.storeUrl)
    }

    @Test
    fun `업데이트 필요 - 최신 versionCode 와 스토어 URL 디코드`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"updateRequired":true,"latestVersionCode":10002,"storeUrl":"https://play.google.com/store/apps/details?id=com.afternote"}}"""

        val data = json.decodeFromString<BaseResponse<AppVersionDto>>(payload).requireData()

        assertTrue(data.updateRequired)
        assertEquals(10002, data.latestVersionCode)
        assertEquals("https://play.google.com/store/apps/details?id=com.afternote", data.storeUrl)
    }

    @Test
    fun `판정 필드 누락 - 기본값으로 우회하지 않고 디코드 실패`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"latestVersionCode":10002,"storeUrl":null}}"""

        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<AppVersionDto>>(payload)
        }
    }

    @Test
    fun `storeUrl 키 누락 - nullable 이어도 필수 응답 키이므로 디코드 실패`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"updateRequired":false,"latestVersionCode":10001}}"""

        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<AppVersionDto>>(payload)
        }
    }
}
