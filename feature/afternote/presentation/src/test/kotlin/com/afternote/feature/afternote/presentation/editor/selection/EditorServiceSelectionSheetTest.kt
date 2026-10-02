package com.afternote.feature.afternote.presentation.editor.selection

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorServiceSelectionSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * 검색 규칙은 시트가 실제로 그리는 행 목록으로 판정한다 — 행은 catalog 순서대로 쌓이므로
     * 활성 클릭 타깃의 트리 순서가 곧 표시 순서다.
     */
    @Test
    fun `검색은 trim과 대소문자 무시 substring을 적용하고 catalog 순서를 유지한다`() {
        val services = listOf("Alpha", "beta", "ALPHABET", "gamma")
        val queryState = TextFieldState()
        composeRule.setContent {
            AfternoteTheme {
                EditorServiceSelectionSheetContent(
                    title = "소셜 네트워크 서비스 선택",
                    type = AfternoteType.SOCIAL_NETWORK,
                    services = services,
                    searchQueryState = queryState,
                    onServiceSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("서비스 검색하기").performTextInput("  pHa  ")
        assertEquals(listOf("Alpha", "ALPHABET"), serviceRowNames())

        composeRule.onNodeWithText("  pHa  ").performTextReplacement("   ")
        assertEquals(services, serviceRowNames())
    }

    /**
     * 카테고리 → 시트 제목 배선. 서비스 선택이 없는 카테고리(추억 노트·재산 처리)는 시트 자체를 열지 않는다.
     */
    @Test
    fun `sheet title은 카테고리별 확정 문구를 쓰고 미지원 카테고리는 시트를 열지 않는다`() {
        val type = mutableStateOf(AfternoteType.SOCIAL_NETWORK)
        composeRule.setContent {
            AfternoteTheme {
                EditorServiceSelectionSheet(
                    visible = true,
                    type = type.value,
                    services = emptyList(),
                    searchQueryState = rememberTextFieldState(),
                    onDismissRequest = {},
                    onServiceSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("소셜 네트워크 서비스 선택").assertExists()

        composeRule.runOnIdle { type.value = AfternoteType.GALLERY_AND_FILES }
        composeRule.onNodeWithText("갤러리 및 파일 서비스 선택").assertExists()

        composeRule.runOnIdle { type.value = AfternoteType.BUSINESS }
        composeRule.onNodeWithText("비즈니스 서비스 선택").assertExists()

        composeRule.runOnIdle { type.value = AfternoteType.MEMORIAL }
        composeRule.onNodeWithText("서비스 선택", substring = true).assertDoesNotExist()

        composeRule.runOnIdle { type.value = AfternoteType.ESTATE }
        composeRule.onNodeWithText("서비스 선택", substring = true).assertDoesNotExist()
    }

    @Test
    fun `검색 UI는 일치 항목만 남기고 결과가 없으면 빈 결과를 표시한다`() {
        val queryState = TextFieldState()
        composeRule.setContent {
            AfternoteTheme {
                EditorServiceSelectionSheetContent(
                    title = "비즈니스 서비스 선택",
                    type = AfternoteType.BUSINESS,
                    services = AfternoteServiceCatalog.businessServices,
                    searchQueryState = queryState,
                    onServiceSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("서비스 검색하기").performTextInput("  OUT  ")
        composeRule.onNodeWithText("outlook").assertIsDisplayed()
        composeRule.onNodeWithText("네이버 메일").assertDoesNotExist()

        composeRule.onNodeWithText("  OUT  ").performTextReplacement("없는 서비스")
        composeRule.onNodeWithText("검색 결과가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("outlook").assertDoesNotExist()
    }

    @Test
    fun `서비스 행은 정확한 display key를 넘기는 48dp 이상 Button이다`() {
        var selected: String? = null
        val queryState = TextFieldState()
        composeRule.setContent {
            AfternoteTheme {
                EditorServiceSelectionSheetContent(
                    title = "소셜 네트워크 서비스 선택",
                    type = AfternoteType.SOCIAL_NETWORK,
                    services = listOf("인스타그램", "페이스북"),
                    searchQueryState = queryState,
                    onServiceSelected = { selected = it },
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val rows =
            composeRule
                .scanEnabledClickTargets()
                .filter { it.name == "인스타그램" || it.name == "페이스북" }
        assertEquals(2, rows.size)
        rows.forEach { row ->
            assertEquals(Role.Button, row.role)
            assertFalse(row.isSmallerThan(MinimumTouchTargetSize))
        }

        composeRule.onNodeWithText("페이스북").performClick()
        assertEquals("페이스북", selected)
    }

    @Test
    fun `접힌 필드는 catalog 밖 기존 custom 값을 보존하고 sheet 열기 callback을 낸다`() {
        var openCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                EditorServiceSelectionField(
                    selectedService = "사내 레거시 서비스",
                    placeholder = "소셜네트워크 선택하기",
                    onClick = { openCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("사내 레거시 서비스").assertIsDisplayed().performClick()

        assertEquals(1, openCalls)
        val field = composeRule.scanEnabledClickTargets().single { it.name == "사내 레거시 서비스" }
        assertEquals(Role.Button, field.role)
        assertFalse(field.isSmallerThan(MinimumTouchTargetSize))
    }

    /** 시트 본문이 그리는 서비스 행 이름을 표시 순서대로 모은다. 검색 입력창은 Button 이 아니라 빠진다. */
    private fun serviceRowNames(): List<String> =
        composeRule
            .scanEnabledClickTargets()
            .filter { it.role == Role.Button }
            .map { it.name }
}
