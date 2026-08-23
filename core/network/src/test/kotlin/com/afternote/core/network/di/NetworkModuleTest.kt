package com.afternote.core.network.di

import com.afternote.core.network.interceptor.ApiErrorInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NetworkModuleTest {
    private val baseClient = NetworkModule.provideBaseOkHttpClient()

    @Test
    fun `재발급 클라이언트 - API 오류 본문 변환 인터셉터를 가장 바깥에 배치`() {
        val apiErrorInterceptor = ApiErrorInterceptor(NetworkModule.provideJson())

        val client =
            NetworkModule.provideRefreshOkHttpClient(
                baseClient = baseClient,
                loggingInterceptor = HttpLoggingInterceptor(),
                apiErrorInterceptor = apiErrorInterceptor,
            )

        assertSame(apiErrorInterceptor, client.interceptors.first())
    }

    @Test
    fun `S3 업로드 클라이언트 - 전체 호출은 10분 안에 종료`() {
        val client = NetworkModule.provideS3UploadOkHttpClient(baseClient, HttpLoggingInterceptor())

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(60_000, client.readTimeoutMillis)
        assertEquals(60_000, client.writeTimeoutMillis)
        assertEquals(600_000, client.callTimeoutMillis)
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
