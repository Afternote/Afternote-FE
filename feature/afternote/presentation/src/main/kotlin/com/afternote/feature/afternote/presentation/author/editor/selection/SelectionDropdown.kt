package com.afternote.feature.afternote.presentation.author.editor.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.EditorSectionLabel

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
 * @param shape 드롭다운 메뉴 컨테이너 모양 (기본: 4.dp 라운드 — Material3 extraSmall 동등)
 */
data class DropdownMenuStyle(
    val menuOffset: Dp = 4.dp,
    val menuBackgroundColor: Color? = null,
    val shadowElevation: Dp = 0.dp,
    val tonalElevation: Dp = 0.dp,
    val shape: Shape = RoundedCornerShape(8.dp),
)

/**
 * @param labelParams Label text, required indicator, and style
 * @param selectedValue Currently selected value
 * @param options List of selectable options
 * @param onValueSelected Callback when an option is selected
 * @param expanded Whether the dropdown menu is currently expanded
 * @param onExpandedChange Callback invoked when the user requests to open/close the menu
 * @param modifier Modifier for the component
 * @param menuStyle Style configuration for the dropdown menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDropdown(
    labelParams: SelectionDropdownLabelParams,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    menuStyle: DropdownMenuStyle = DropdownMenuStyle(),
) {
    val menuBackgroundResolved = menuStyle.menuBackgroundColor ?: AfternoteDesign.colors.white

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp),
    ) {
        // 라벨
        EditorSectionLabel(
            text = labelParams.label,
            isRequired = labelParams.isRequired,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
        )

        // Material 3 ExposedDropdownMenuBox: 앵커–메뉴 너비·접근성, 서브컴포지션 없이 처리
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                        .bottomBorder(color = AfternoteDesign.colors.gray3, width = 0.58.dp)
                        .clickable(role = Role.DropdownList) { onExpandedChange(!expanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedValue,
                    style =
                        AfternoteDesign.typography.bodyBase.copy(
                            color = AfternoteDesign.colors.gray8,
                        ),
                )

                Icon(
                    painter = painterResource(R.drawable.feature_afternote_ic_dropdown_vector),
                    contentDescription = stringResource(R.string.afternote_editor_content_description_dropdown),
                    tint = AfternoteDesign.colors.gray8,
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.offset(y = menuStyle.menuOffset),
                shape = menuStyle.shape,
                containerColor = menuBackgroundResolved,
                shadowElevation = menuStyle.shadowElevation,
                tonalElevation = menuStyle.tonalElevation,
            ) {
                SelectionDropdownMenuItems(
                    options = options,
                    onSelect = { selected ->
                        onValueSelected(selected)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

/**
 * 펼친 메뉴 항목 리스트 — Popup 컨텍스트 밖에서도 그릴 수 있어 Preview로 단독 확인 가능.
 */
@Composable
private fun SelectionDropdownMenuItems(
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = option,
                        style =
                            AfternoteDesign.typography.bodyBase.copy(
                                color = AfternoteDesign.colors.gray9,
                            ),
                    )
                },
                onClick = { onSelect(option) },
                contentPadding = PaddingValues(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionDropdownPreview() {
    AfternoteTheme {
        val social = stringResource(R.string.afternote_editor_category_social)
        var expanded by remember { mutableStateOf(false) }
        SelectionDropdown(
            labelParams =
                SelectionDropdownLabelParams(
                    label = stringResource(R.string.afternote_editor_label_category),
                ),
            selectedValue = social,
            options =
                listOf(
                    social,
                    stringResource(R.string.afternote_editor_category_gallery),
                    stringResource(R.string.afternote_editor_category_memorial),
                ),
            onValueSelected = {},
            expanded = expanded,
            onExpandedChange = { expanded = it },
        )
    }
}

@Preview(showBackground = true, name = "Expanded menu items")
@Composable
private fun SelectionDropdownMenuItemsPreview() {
    AfternoteTheme {
        SelectionDropdownMenuItems(
            options =
                listOf(
                    "인스타그램",
                    "페이스북",
                    "직접 추가하기",
                ),
            onSelect = {},
        )
    }
}
