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
        var unavailableCount = 0

        urls.forEach { url ->
            launchReceivedMemorialVideo(
                videoUrl = url,
                startActivity = { startedIntents += it },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(urls, startedIntents.map { it.dataString })
        assertTrue(startedIntents.all { it.action == Intent.ACTION_VIEW })
        assertEquals(0, unavailableCount)
    }

    @Test
    fun `http와 https가 아닌 URL이나 잘못된 URL은 실행하지 않는다`() {
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
        var unavailableCount = 0

        rejectedUrls.forEach { url ->
            launchReceivedMemorialVideo(
                videoUrl = url,
                startActivity = { startedCount += 1 },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(0, startedCount)
        assertEquals(rejectedUrls.size, unavailableCount)
    }

    @Test
    fun `외부 실행이 거부되면 안내 콜백을 호출한다`() {
        val failures =
            listOf(
                ActivityNotFoundException("no player"),
                SecurityException("blocked"),
                IllegalArgumentException("bad uri"),
            )
        var unavailableCount = 0

        failures.forEach { failure ->
            launchReceivedMemorialVideo(
                videoUrl = "https://cdn.example.com/memorial.mp4",
                startActivity = { throw failure },
                onUnavailable = { unavailableCount += 1 },
            )
        }

        assertEquals(failures.size, unavailableCount)
    }
}
