package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 플레이리스트 전체보기가 열람 화면의 헤더만 두는지 (#620).
 *
 * 종전에는 곡을 골라 담는 화면(AddSongScreen)의 검색 헤더를 그대로 써서, 고인의 플레이리스트에
 * 곡을 더할 수 있는 것처럼 보였다. 발신자 열람 화면과 같은 "총 N곡" 으로 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverPlaylistHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val songs =
        listOf(
            PlaylistSongDisplay(selectionKey = "1", title = "노래 1", artist = "아티스트 1"),
            PlaylistSongDisplay(selectionKey = "2", title = "노래 2", artist = "아티스트 2"),
        )

    private fun renderScreen() {
        composeRule.setContent {
            AfternoteTheme {
                MemorialPlaylistScreen(
                    songs = songs,
                    onBackClick = {},
                    senderName = "서연",
                )
            }
        }
    }

    @Test
    fun `수신자 플레이리스트에는 곡 검색 입력이 없다`() {
        renderScreen()

        val searchLabel = composeRule.activity.getString(R.string.afternote_song_search_label)
        val searchPlaceholder = composeRule.activity.getString(R.string.afternote_song_search_placeholder)
        composeRule.onNodeWithText(searchLabel).assertDoesNotExist()
        composeRule.onNodeWithText(searchPlaceholder).assertDoesNotExist()
    }

    @Test
    fun `수신자 플레이리스트 헤더는 곡 수를 보여준다`() {
        renderScreen()

        val songCount = composeRule.activity.getString(R.string.afternote_receiver_playlist_song_count_format, songs.size)
        composeRule.onNodeWithText(songCount).assertIsDisplayed()
    }
}
