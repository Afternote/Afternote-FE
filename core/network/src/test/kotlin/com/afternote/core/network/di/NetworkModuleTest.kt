package com.afternote.core.network.di

import com.afternote.core.network.calladapter.ApiErrorCallAdapterFactory
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleTest {
    private val baseClient = NetworkModule.provideBaseOkHttpClient()

    @Test
    fun `재발급 클라이언트 - HTTP 오류 변환 인터셉터 없이 로깅만 배치`() {
        val loggingInterceptor = HttpLoggingInterceptor()

        val client =
            NetworkModule.provideRefreshOkHttpClient(
                baseClient = baseClient,
                loggingInterceptor = loggingInterceptor,
            )

        assertEquals(listOf(loggingInterceptor), client.interceptors)
    }

    @Test
    fun `메인과 재발급 Retrofit - 같은 API 오류 CallAdapter를 등록`() {
        val json = NetworkModule.provideJson()
        val factory = ApiErrorCallAdapterFactory(json)
        val mainRetrofit = NetworkModule.provideRetrofit(baseClient, json, factory)
        val refreshRetrofit = ServiceModule.provideRefreshRetrofit(baseClient, json, factory)

        assertTrue(mainRetrofit.callAdapterFactories().contains(factory))
        assertTrue(refreshRetrofit.callAdapterFactories().contains(factory))
    }

    @Test
    fun `S3 업로드 클라이언트 - 전체 호출은 10분 안에 종료`() {
        val client = NetworkModule.provideS3UploadOkHttpClient(baseClient, HttpLoggingInterceptor())

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(60_000, client.readTimeoutMillis)
        assertEquals(60_000, client.writeTimeoutMillis)
        assertEquals(600_000, client.callTimeoutMillis)
    }

    /**
     * Coil 기본 로더 대신 이 클라이언트를 쓰는 근거가 이 두 가지뿐이라, 둘 중 하나가 사라지면
     * 체인 전체가 순수 비용이 된다. 인터셉터 목록을 통째로 단언해 근거 없는 헤더 부착도 함께 막는다.
     */
    @Test
    fun `Coil 클라이언트 - 로깅과 호출 상한만 얹고 그 밖의 인터셉터는 없음`() {
        val loggingInterceptor = HttpLoggingInterceptor()

        val client = NetworkModule.provideCoilImageOkHttpClient(baseClient, loggingInterceptor)

        assertEquals(30_000, client.callTimeoutMillis)
        assertEquals(listOf(loggingInterceptor), client.interceptors)
    }

    @Test
    fun `파생 클라이언트 - base 와 커넥션 풀·디스패처를 공유`() {
        val coilClient = NetworkModule.provideCoilImageOkHttpClient(baseClient, HttpLoggingInterceptor())
        val s3Client = NetworkModule.provideS3UploadOkHttpClient(baseClient, HttpLoggingInterceptor())

        assertSame(baseClient.connectionPool, coilClient.connectionPool)
        assertSame(baseClient.dispatcher, coilClient.dispatcher)
        assertSame(baseClient.connectionPool, s3Client.connectionPool)
        assertSame(baseClient.dispatcher, s3Client.dispatcher)
    }
}
