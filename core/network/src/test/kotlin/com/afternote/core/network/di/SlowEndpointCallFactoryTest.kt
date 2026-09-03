package com.afternote.core.network.di

import com.afternote.core.network.calladapter.ApiErrorCallAdapterFactory
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 느린 엔드포인트만 여유 있는 상한으로 태우는지 (#1122).
 *
 * 주간 리포트(`GET /api/v1/mind-record`)는 같은 계정·같은 시각에 일기 목록이 4.0초일 때
 * 16.9 · 20.2 · 25.9초가 걸렸다(실측 2026-08-25). 10초 read timeout 에 매번 걸려 화면이
 * **항상** 실패했고, 재시도도 같은 자리에서 끊겼다.
 *
 * 인터셉터로는 `callTimeout` 을 못 늘린다 — read 만 늘리면 30초 호출 상한에 다시 걸린다.
 * 그래서 파생 클라이언트로 가른다. 이 테스트는 «가르는 것» 과 «나머지는 그대로» 를 함께 고정한다.
 *
 * 가르는 팩토리는 [NetworkModule] 파일 안에만 사는 구현이라, 검증은 모듈이 실제로 내놓는
 * Retrofit 의 call factory 로 한다 — «앱이 쓰는 그 객체» 를 그대로 재는 경로다 (#1672).
 */
class SlowEndpointCallFactoryTest {
    private val default =
        OkHttpClient
            .Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

    private val factory: Call.Factory =
        NetworkModule
            .provideRetrofit(
                okHttpClient = default,
                json = NetworkModule.provideJson(),
                apiErrorCallAdapterFactory = ApiErrorCallAdapterFactory(NetworkModule.provideJson()),
            ).callFactory()

    @Test
    fun `주간 리포트는 늘어난 상한으로 나간다`() {
        val call = factory.newCall(request("https://example.com/api/v1/mind-record?date=2026-08-24"))

        assertEquals(60_000, readTimeoutMillisOf(call))
    }

    @Test
    fun `다른 경로는 기본 상한 그대로다`() {
        val call = factory.newCall(request("https://example.com/api/v1/diary?yearMonth=2026-08"))

        assertEquals(10_000, readTimeoutMillisOf(call))
    }

    @Test
    fun `호출 상한도 함께 늘어난다`() {
        // read 만 늘리면 30초 callTimeout 에 다시 걸린다 — 25.9초 실측이 그 경계에 붙어 있다.
        val slow = factory.newCall(request("https://example.com/api/v1/mind-record?date=2026-08-24"))

        assertEquals(90_000L, slow.timeout().timeoutNanos() / 1_000_000)
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()

    private fun readTimeoutMillisOf(call: okhttp3.Call): Int = (call as okhttp3.internal.connection.RealCall).client.readTimeoutMillis
}
