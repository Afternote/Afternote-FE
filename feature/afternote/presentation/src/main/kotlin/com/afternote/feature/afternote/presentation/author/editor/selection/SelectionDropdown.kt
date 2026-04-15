package com.afternote.feature.afternote.presentation.author.editor.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.Label
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

private const val CATEGORY_SOCIAL_NETWORK = "소셜네트워크"
private const val CATEGORY_GALLERY_AND_FILE_PREVIEW = "갤러리 및 파일"
private const val CATEGORY_MEMORIAL_PREVIEW = "추모 가이드라인"

/**
 * Label configuration for [SelectionDropdown].
 */
data class SelectionDropdownLabelParams(
    val label: String,
    val isRequired: Boolean = false,
)

/**
 * 드롭다운 메뉴 스타일 설정
 *
 * @param menuOffset 앵커 대비 메뉴 패널에 더할 수직 간격 (기본: 4.dp, 메뉴 modifier 세로 offset으로 반영)
 * @param menuBackgroundColor 드롭다운 메뉴 배경색 (기본: AfternoteDesign.colors.white)
 * @param shadowElevation 드롭다운 메뉴 그림자 elevation (기본: 0.dp)
 * @param tonalElevation 드롭다운 메뉴 톤 elevation (기본: 0.dp)
 */
data class DropdownMenuStyle(
    val menuOffset: Dp = 4.dp,
    val menuBackgroundColor: Color? = null,
    val shadowElevation: Dp = 0.dp,
    val tonalElevation: Dp = 0.dp,
)

/**
 * @param modifier Modifier for the component
 * @param labelParams Label text, required indicator, and style
 * @param selectedValue Currently selected value
 * @param options List of selectable options
 * @param onValueSelected Callback when an option is selected
 * @param menuStyle Style configuration for the dropdown menu
 * @param state State holder for the dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDropdown(
    modifier: Modifier = Modifier,
    labelParams: SelectionDropdownLabelParams,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    menuStyle: DropdownMenuStyle = DropdownMenuStyle(),
    state: SelectionDropdownState = rememberSelectionDropdownState(),
) {
    val menuBackgroundResolved = menuStyle.menuBackgroundColor ?: AfternoteDesign.colors.white

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                space = 8.dp,
            ),
    ) {
        // 라벨
        Label(
            text = labelParams.label,
            isRequired = labelParams.isRequired,
        )

        // 드롭다운 필드 (메뉴 너비는 BoxWithConstraints 제약으로 맞춤 — 측정 후 state 갱신 없음)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val menuModifier =
                if (constraints.hasBoundedWidth) {
                    Modifier.width(maxWidth)
                } else {
                    Modifier.fillMaxWidth()
                }
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { state.expanded = !state.expanded }
                            .bottomBorder(color = AfternoteDesign.colors.gray3, width = 0.5.dp)
                            .padding(all = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = selectedValue,
                            style =
                                AfternoteDesign.typography.bodyBase.copy(
                                    lineHeight = 20.sp,
                                    color = AfternoteDesign.colors.gray8,
                                ),
                        )

                        Image(
                            painter = painterResource(R.drawable.feature_afternote_ic_dropdown_vector),
                            contentDescription = "드롭다운",
                        )
                    }
                }

                // 드롭다운 메뉴
                DropdownMenu(
                    expanded = state.expanded,
                    onDismissRequest = { state.expanded = false },
                    offset = DpOffset(x = 0.dp, y = menuStyle.menuOffset),
                    containerColor = menuBackgroundResolved,
                    shadowElevation = menuStyle.shadowElevation,
                    tonalElevation = menuStyle.tonalElevation,
                    modifier = menuModifier,
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    style =
                                        AfternoteDesign.typography.textField.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = AfternoteDesign.colors.gray9,
                                            textAlign = TextAlign.Center,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            onClick = {
                                onValueSelected(option)
                                state.expanded = false
                            },
                            contentPadding = PaddingValues(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionDropdownPreview() {
    AfternoteTheme {
        SelectionDropdown(
            labelParams = SelectionDropdownLabelParams(label = "종류"),
            selectedValue = CATEGORY_SOCIAL_NETWORK,
            options =
                listOf(
                    CATEGORY_SOCIAL_NETWORK,
                    CATEGORY_GALLERY_AND_FILE_PREVIEW,
                    CATEGORY_MEMORIAL_PREVIEW,
                ),
            onValueSelected = {},
        )
    }
}
