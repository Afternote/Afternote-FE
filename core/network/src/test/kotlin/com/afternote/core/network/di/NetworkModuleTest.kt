package com.afternote.core.network.di

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `S3 업로드 클라이언트 - 전체 호출은 10분 안에 종료`() {
        val client = NetworkModule.provideS3UploadOkHttpClient(HttpLoggingInterceptor())

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(60_000, client.readTimeoutMillis)
        assertEquals(60_000, client.writeTimeoutMillis)
        assertEquals(600_000, client.callTimeoutMillis)
    }
}
