package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 곡 검색 진행 상태가 화면에 도달하는지에 대한 회귀 가드 (#705).
 *
 * 종전에는 [AddSongUiState.isLoading] 을 아무도 소비하지 않아 «아직 오는 중» 과 «결과 0건» 이
 * 똑같은 빈 목록으로 보였다 — 사용자는 검색이 안 되는 화면으로 읽는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AddSongSearchProgressTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `검색이 왕복 중이면 진행 표시자를 그린다`() {
        setScreen(AddSongUiState(searchQuery = "노래", isLoading = true))

        composeRule.onNodeWithTag(ADD_SONG_SEARCH_PROGRESS_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `결과가 도착하면 진행 표시자를 걷는다`() {
        setScreen(
            AddSongUiState(
                searchQuery = "노래",
                isLoading = false,
                songs = listOf(PlaylistSongDisplay("search:0", "노래", "가수", null)),
            ),
        )

        composeRule.onNodeWithTag(ADD_SONG_SEARCH_PROGRESS_TEST_TAG).assertDoesNotExist()
    }

    private fun setScreen(uiState: AddSongUiState) {
        composeRule.setContent {
            AfternoteTheme {
                AddSongScreen(
                    uiState = uiState,
                    onSearchQueryChange = {},
                    onErrorConsumed = {},
                    onBackClick = {},
                    onSongsAdded = {},
                )
            }
        }
    }
}
