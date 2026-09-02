package com.afternote.feature.afternote.presentation.editor.selection

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorServiceSelectionSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `검색은 trim과 대소문자 무시 substring을 적용하고 catalog 순서를 유지한다`() {
        val services = listOf("Alpha", "beta", "ALPHABET", "gamma")

        assertEquals(
            listOf("Alpha", "ALPHABET"),
            filterEditorServiceOptions(services, "  pHa  "),
        )
        assertEquals(services, filterEditorServiceOptions(services, " \n "))
    }

    @Test
    fun `카테고리별 sheet title은 확정 문구를 사용한다`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals(
            "소셜 네트워크 서비스 선택",
            context.getString(requireNotNull(AfternoteType.SOCIAL_NETWORK.serviceSelectionSheetTitleResOrNull())),
        )
        assertEquals(
            "갤러리 및 파일 서비스 선택",
            context.getString(requireNotNull(AfternoteType.GALLERY_AND_FILES.serviceSelectionSheetTitleResOrNull())),
        )
        assertEquals(
            "비즈니스 서비스 선택",
            context.getString(requireNotNull(AfternoteType.BUSINESS.serviceSelectionSheetTitleResOrNull())),
        )
        assertNull(AfternoteType.MEMORIAL.serviceSelectionSheetTitleResOrNull())
        assertNull(AfternoteType.ESTATE.serviceSelectionSheetTitleResOrNull())
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
}
