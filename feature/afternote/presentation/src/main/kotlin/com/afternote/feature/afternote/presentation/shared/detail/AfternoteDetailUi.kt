package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import androidx.compose.ui.window.Popup as ComposePopup

/**
 * 상세 화면에서 사용하는 회색 배경 + 둥근 모서리 컨테이너.
 *
 * Material3 [androidx.compose.material3.Card] 는 elevation·ripple·surface tint 등
 * 이 화면에서 쓰지 않는 기본값이 얹혀 있어 커스텀 디자인 시스템과 충돌한다.
 * 단순 Column 위에 `clip + background + padding` 을 체이닝해 렌더링 비용을 줄이고
 * [DetailCard] 와 일관된 스타일링 방식을 유지한다.
 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AfternoteDesign.colors.gray2)
                .padding(16.dp),
        content = content,
    )
}

@Composable
fun ProcessingMethodItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = AfternoteDesign.typography.bodyLargeR,
        color = AfternoteDesign.colors.gray9,
    )
}

/**
 * 수신자 목록 카드. 애프터노트 상세 화면(갤러리/소셜/추모 가이드라인)에서 공통 사용.
 * 수신자가 없으면 아무것도 표시하지 않음.
 */
@Composable
fun ReceiversCard(
    receivers: List<ReceiverUiModel>,
    modifier: Modifier = Modifier,
) {
    if (receivers.isEmpty()) return

    InfoCard(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.afternote_detail_receivers_label),
                style =
                    AfternoteDesign.typography.textField.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.gray9,
                    ),
            )
            receivers.forEach { receiver ->
                ReceiverDetailItem(receiver = receiver)
            }
        }
    }
}

@Composable
private fun ReceiverDetailItem(
    receiver: ReceiverUiModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.feature_afternote_img_recipient_profile),
            contentDescription = stringResource(R.string.feature_afternote_content_description_recipient_profile),
            modifier =
                Modifier
                    .size(58.dp)
                    .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column {
            Text(
                text = receiver.name,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.black,
                    ),
            )
            Text(
                text = receiver.label,
                style =
                    AfternoteDesign.typography.captionLargeR.copy(
                        color = AfternoteDesign.colors.gray8,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiversCardPreview() {
    AfternoteTheme {
        ReceiversCard(
            receivers =
                listOf(
                    ReceiverUiModel(
                        id = "1",
                        name = "황규운",
                        label = "친구",
                    ),
                    ReceiverUiModel(
                        id = "2",
                        name = "김소희",
                        label = "가족",
                    ),
                ),
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    serviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.feature_afternote_dialog_delete_title)
    val body = stringResource(R.string.feature_afternote_dialog_delete_body, serviceName)
    Popup(
        type = PopupType.Variant2,
        message = "$title\n\n$body",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.feature_afternote_dialog_delete_cancel),
        confirmText = stringResource(R.string.feature_afternote_dialog_delete_confirm),
    )
}

@Preview(showBackground = true)
@Composable
private fun DeleteConfirmDialogPreview() {
    AfternoteTheme {
        DeleteConfirmDialog(
            serviceName = "인스타그램",
            onDismiss = {},
            onConfirm = {},
        )
    }
}

/**
 * 상세 화면 우측 상단 "더보기" 버튼에서 펼쳐지는 커스텀 드롭다운.
 *
 * Material3 [androidx.compose.material3.DropdownMenu] 는 그림자/애니메이션 시작점/
 * 아이템 최소 높이(48dp) 등이 가이드라인에 맞게 강제돼 있어 우회 시 코드가 지저분해진다.
 * 둥글기·폰트·그림자·패딩·등장 위치를 100% 제어하기 위해 순수 [ComposePopup] 위에
 * 디자인 시스템 토큰만 얹어 직접 구성했다.
 */
@Composable
fun EditDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    showEditItem: Boolean = true,
) {
    if (!expanded) return

    ComposePopup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = 0),
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier =
                Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = AfternoteDesign.colors.black.copy(alpha = 0.15f),
                    ).clip(RoundedCornerShape(8.dp))
                    .background(AfternoteDesign.colors.white),
        ) {
            if (showEditItem) {
                CustomDropdownItem(
                    text = stringResource(R.string.feature_afternote_menu_edit),
                    onClick = {
                        onDismissRequest()
                        onEditClick()
                    },
                )
            }
            CustomDropdownItem(
                text = stringResource(R.string.feature_afternote_menu_delete_record),
                onClick = {
                    onDismissRequest()
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
private fun CustomDropdownItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 26.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.align(Alignment.CenterStart),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}
