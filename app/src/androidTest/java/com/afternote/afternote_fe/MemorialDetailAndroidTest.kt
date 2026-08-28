package com.afternote.afternote_fe

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.detail.MemorialDetailContent
import com.afternote.feature.afternote.presentation.author.detail.MemorialDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemorialDetailAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun memorialVideoCardTap_forwardsExactUrl() {
        val videoUrl = "https://cdn.example.com/memorial.mp4"
        var clickedUrl: String? = null

        composeRule.setContent {
            AfternoteTheme {
                MemorialDetailScreen(
                    onBackClick = {},
                    content =
                        MemorialDetailContent(
                            userName = "서영",
                            memorialVideoUrl = videoUrl,
                        ),
                    onVideoClick = { clickedUrl = it },
                )
            }
        }

        composeRule
            .onNodeWithTag("memorialVideoCard")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle { assertEquals(videoUrl, clickedUrl) }
    }
}
