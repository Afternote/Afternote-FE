package com.afternote.feature.mindrecord.presentation.screen.receiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter
import com.afternote.feature.mindrecord.presentation.viewmodel.SortOrder

/**
 * 수신자 마음의 기록 필터 바텀시트 (디자인 노드 1727-25357 / 25054 / 23247 / 23886).
 *
 * 4 가지 상태 통합 구현:
 *   default      : 정렬 칩 + 빈 텍스트필드 + 빈 캘린더
 *   하루 선택    : 캘린더 셀 1개 하이라이트 → fromDate = toDate = 선택일
 *   기간 선택    : 캘린더 행 단위 하이라이트 → fromDate..toDate 범위
 *   키보드 조회  : 텍스트필드 포커스 시 Numeric 키패드 노출 (시스템 IME 사용)
 *
 * 캘린더는 후속 커밋에서 [com.afternote.core.ui.calendar.BottomSheetCalendar] 로 교체 예정.
 * 1차는 텍스트필드 직접 입력만 동작하고 캘린더 영역은 placeholder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverMindRecordFilterSheet(
    current: ReceiverMindRecordFilter,
    onDismiss: () -> Unit,
    onApply: (ReceiverMindRecordFilter) -> Unit,
    onReset: () -> Unit,
) {
    var sort by remember { mutableStateOf(current.sortOrder) }
    var fromText by remember { mutableStateOf(current.fromDate.orEmpty()) }
    var toText by remember { mutableStateOf(current.toDate.orEmpty()) }

    val dragHandleLabel = stringResource(R.string.mindrecord_receiver_filter_drag_handle_cd)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.88f),
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(AfternoteDesign.colors.gray2)
                        // material3 는 dragHandle 슬롯에 시트 접기·펴기 클릭 액션을 얹는다. 기본
                        // `BottomSheetDefaults.DragHandle` 대신 자체 Box 를 넣으면서 그 이름이
                        // 통째로 비었다 — 스캐너 실측으로 드러났다 (#1179 리뷰).
                        .semantics { contentDescription = dragHandleLabel },
            )
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "발송 날짜",
                    style = AfternoteDesign.typography.bodyLargeB,
                    color = AfternoteDesign.colors.gray9,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            SortSection(sort = sort, onSortChange = { sort = it })
            Spacer(modifier = Modifier.height(24.dp))

            DateRangeSection(
                fromText = fromText,
                toText = toText,
                onFromChange = { fromText = it },
                onToChange = { toText = it },
            )
            Spacer(modifier = Modifier.height(24.dp))

            CalendarPlaceholder()
            Spacer(modifier = Modifier.height(24.dp))

            ActionRow(
                onReset = {
                    sort = current.sortOrder
                    fromText = ""
                    toText = ""
                    onReset()
                },
                onApply = {
                    onApply(
                        ReceiverMindRecordFilter(
                            sortOrder = sort,
                            fromDate = fromText.ifBlank { null },
                            toDate = toText.ifBlank { null },
                        ),
                    )
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SortSection(
    sort: SortOrder,
    onSortChange: (SortOrder) -> Unit,
) {
    Column {
        Text(text = "정렬", style = AfternoteDesign.typography.bodySmallB, color = AfternoteDesign.colors.gray9)
        Spacer(modifier = Modifier.height(8.dp))
        // 둘 중 하나를 고르는 그룹이다 — 없으면 스크린리더가 「N 개 중 M 번째」를 못 읽는다 (#1179).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            SortOrder.entries.forEach { order ->
                SortChip(
                    label = order.label,
                    isSelected = sort == order,
                    onClick = { onSortChange(order) },
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.white
    val fg = if (isSelected) AfternoteDesign.colors.white else AfternoteDesign.colors.gray9
    Text(
        text = label,
        style = AfternoteDesign.typography.captionLargeR,
        color = fg,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(50))
                .background(bg)
                // 색만 바뀌던 칩이라 **선택 상태가 semantics 에 없었다** — 눈으로만 구분됐다 (#1179).
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun DateRangeSection(
    fromText: String,
    toText: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
) {
    Column {
        Text(text = "기간 조회", style = AfternoteDesign.typography.bodySmallB, color = AfternoteDesign.colors.gray9)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateField(value = fromText, onChange = onFromChange, modifier = Modifier.weight(1f))
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 8.dp)
                        .width(11.dp)
                        .height(1.dp)
                        .background(AfternoteDesign.colors.gray4),
            )
            DateField(value = toText, onChange = onToChange, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DateField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        keyboardOptions =
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
            ),
        textStyle = AfternoteDesign.typography.bodyBase,
        decorationBox = { inner ->
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(6.dp))
                        .background(AfternoteDesign.colors.white)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "YYYY.MM.DD",
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun CalendarPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "캘린더 (후속 PR 에서 BottomSheetCalendar 연동)",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Composable
private fun ActionRow(
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, AfternoteDesign.colors.gray2, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "초기화", style = AfternoteDesign.typography.bodySmallB, color = AfternoteDesign.colors.gray9)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AfternoteDesign.colors.gray9)
                    .clickable(role = Role.Button, onClick = onApply)
                    .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "적용", style = AfternoteDesign.typography.bodySmallB, color = AfternoteDesign.colors.white)
        }
    }
}
