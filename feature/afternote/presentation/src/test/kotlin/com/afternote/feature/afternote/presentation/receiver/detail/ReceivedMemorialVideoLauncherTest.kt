package com.afternote.feature.afternote.presentation.receiver.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivedMemorialVideoLauncherTest {
    @Test
    fun `http와 https URL은 ACTION_VIEW로 직접 실행한다`() {
        val urls =
            listOf(
                "https://cdn.example.com/memorial.mp4",
                "http://cdn.example.com/memorial.mp4",
            )
        val startedIntents = mutableListOf<Intent>()
        var rejectedCount = 0
        var unavailableCount = 0

        urls.forEach { url ->
            launchReceivedMemorialVideo(
                videoUrl = url,
                startActivity = { startedIntents += it },
                onRejected = { rejectedCount += 1 },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(urls, startedIntents.map { it.dataString })
        assertTrue(startedIntents.all { it.action == Intent.ACTION_VIEW })
        assertEquals(0, rejectedCount)
        assertEquals(0, unavailableCount)
    }

    @Test
    fun `http와 https가 아닌 URL이나 잘못된 URL은 실행하지 않고 거부로 안내한다`() {
        val rejectedUrls =
            listOf(
                "",
                "not a url",
                "ftp://cdn.example.com/memorial.mp4",
                "content://videos/memorial",
                "javascript:alert(1)",
                "intent://memorial#Intent;scheme=https;end",
                "tel:0212345678",
                "https:///missing-host.mp4",
            )
        var startedCount = 0
        var rejectedCount = 0
        var unavailableCount = 0

        rejectedUrls.forEach { url ->
            launchReceivedMemorialVideo(
                videoUrl = url,
                startActivity = { startedCount += 1 },
                onRejected = { rejectedCount += 1 },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(0, startedCount)
        assertEquals(rejectedUrls.size, rejectedCount)
        // URL 이 막힌 것을 «재생할 앱이 없다» 로 안내하면 거짓이다 — 갈래가 섞이지 않아야 한다.
        assertEquals(0, unavailableCount)
    }

    @Test
    fun `외부 실행이 거부되면 실행 불가로 안내한다`() {
        val failures =
            listOf(
                ActivityNotFoundException("no player"),
                SecurityException("blocked"),
                IllegalArgumentException("bad uri"),
            )
        var rejectedCount = 0
        var unavailableCount = 0

        failures.forEach { failure ->
            launchReceivedMemorialVideo(
                videoUrl = "https://cdn.example.com/memorial.mp4",
                startActivity = { throw failure },
                onRejected = { rejectedCount += 1 },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(failures.size, unavailableCount)
        // 실행까지 갔다가 막힌 것을 «주소가 잘못됐다» 로 안내하면 거짓이다.
        assertEquals(0, rejectedCount)
    }
}
