package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

        composeRule.onNode(indeterminateProgress).assertIsDisplayed()
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

        composeRule.onNode(indeterminateProgress).assertDoesNotExist()
    }

    // 표시자를 테스트 태그가 아니라 진행 시맨틱스로 잡는다 — 태그를 위해 프로덕션 선언을
    // 공개하면 visibility 가드(#1678)가 잡고, 무엇보다 스크린리더가 읽는 계약이 곧 표식이다.
    private val indeterminateProgress get() = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

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
