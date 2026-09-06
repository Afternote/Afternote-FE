package com.afternote.feature.afternote.presentation.editor.selection

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.EditorSectionLabel
import com.afternote.feature.afternote.presentation.shared.model.AfternoteServiceDisplay

/** 서비스 선택 시트를 여는 접힌 필드. 기존 선택값이 고정 카탈로그 밖의 값이어도 그대로 표시한다. */
@Composable
internal fun EditorServiceSelectionField(
    selectedService: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EditorSectionLabel(
            text = stringResource(R.string.afternote_editor_label_service_name),
            isRequired = false,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray7,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .bottomBorder(color = AfternoteDesign.colors.gray3, width = 0.58.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val showPlaceholder = selectedService.isNullOrBlank()
            Text(
                text = if (showPlaceholder) placeholder else selectedService.orEmpty(),
                modifier = Modifier.weight(1f),
                style = AfternoteDesign.typography.bodyBase,
                color = if (showPlaceholder) AfternoteDesign.colors.gray5 else AfternoteDesign.colors.gray8,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.afternote_ic_dropdown_vector),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray8,
            )
        }
    }
}

/**
 * 고정 서비스 카탈로그를 검색하고 선택하는 공용 에디터 시트.
 *
 * 닫기·선택 뒤 query 초기화는 호출자가 소유한 [TextFieldState]와 `onDismissRequest` 계약으로
 * 처리한다. 선택 콜백은 정확한 catalog display key를 넘긴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorServiceSelectionSheet(
    visible: Boolean,
    type: AfternoteType,
    services: List<String>,
    searchQueryState: TextFieldState,
    onDismissRequest: () -> Unit,
    onServiceSelected: (String) -> Unit,
) {
    if (!visible) return
    val titleRes = type.serviceSelectionSheetTitleResOrNull() ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AfternoteDesign.colors.white,
        dragHandle = null,
    ) {
        EditorServiceSelectionSheetContent(
            title = stringResource(titleRes),
            type = type,
            services = services,
            searchQueryState = searchQueryState,
            onServiceSelected = onServiceSelected,
        )
    }
}

/** Popup 윈도 밖에서도 Preview·Robolectric 검증이 가능한 시트 본문. */
@Composable
internal fun EditorServiceSelectionSheetContent(
    title: String,
    type: AfternoteType,
    services: List<String>,
    searchQueryState: TextFieldState,
    onServiceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredServices =
        remember(services, searchQueryState.text) {
            filterEditorServiceOptions(services, searchQueryState.text)
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AfternoteDesign.colors.gray3),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(Modifier.height(20.dp))
        AfternoteTextField(
            state = searchQueryState,
            type = TextFieldType.Search,
            placeholder = stringResource(R.string.afternote_editor_service_search_placeholder),
            imeAction = ImeAction.Search,
        )
        Spacer(Modifier.height(12.dp))

        if (filteredServices.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.afternote_editor_service_search_empty),
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray6,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
            ) {
                items(filteredServices, key = { it }) { service ->
                    EditorServiceSelectionRow(
                        serviceName = service,
                        type = type,
                        onClick = { onServiceSelected(service) },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EditorServiceSelectionRow(
    serviceName: String,
    type: AfternoteType,
    onClick: () -> Unit,
) {
    val service = remember(serviceName, type) { AfternoteServiceDisplay.fromService(serviceName, type) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(service.iconResId),
            contentDescription = null,
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = service.serviceName,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

/** trim한 query의 대소문자를 무시한 부분 문자열 검색. 원본 catalog 순서는 바꾸지 않는다. */
internal fun filterEditorServiceOptions(
    services: List<String>,
    rawQuery: CharSequence,
): List<String> {
    val query = rawQuery.toString().trim()
    return if (query.isEmpty()) {
        services
    } else {
        services.filter { service -> service.contains(query, ignoreCase = true) }
    }
}

@StringRes
internal fun AfternoteType.serviceSelectionSheetTitleResOrNull(): Int? =
    when (this) {
        AfternoteType.SOCIAL_NETWORK -> R.string.afternote_editor_service_sheet_title_social
        AfternoteType.GALLERY_AND_FILES -> R.string.afternote_editor_service_sheet_title_gallery
        AfternoteType.BUSINESS -> R.string.afternote_editor_service_sheet_title_business
        AfternoteType.MEMORIAL, AfternoteType.ESTATE -> null
    }
