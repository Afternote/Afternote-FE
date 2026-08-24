package com.afternote.feature.receiver.presentation.deliveryverification.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

/**
 * 서류 업로드 소스 선택 BottomSheet(design 7) — 6 의 입력 박스 클릭 시 표시.
 *
 * 헤더 "미디어 추가하기" + 2 옵션 (이미지·파일). 디자인은 옵션 좌측에 아이콘이 들어가 있어
 * timeletter 의 `MediaBottomSheetContent` 와 동일한 패턴을 따른다 — 아이콘 리소스는 타임레터 모듈의
 * `R.drawable.ic_image` / `ic_file` 를 cross-feature 가져올 수 없어 작성자 측 갤러리 아이콘으로 대체.
 * 별도 receiver 아이콘 리소스 추가는 디자인 확정 시 후속.
 */
@Composable
fun DocumentSourceBottomSheet(
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
    sheetHeight: Dp? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .then(if (sheetHeight != null) Modifier.height(sheetHeight) else Modifier),
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
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.receiver_verify_bottom_sheet_header),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.height(18.dp))
        BottomSheetOption(
            iconRes = com.afternote.core.ui.R.drawable.core_ui_ic_image,
            label = stringResource(R.string.receiver_verify_add_image),
            onClick = onPickImage,
        )
        BottomSheetOption(
            iconRes = com.afternote.core.ui.R.drawable.core_ui_ic_file,
            label = stringResource(R.string.receiver_verify_add_file),
            onClick = onPickFile,
        )
        if (sheetHeight != null) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomSheetOption(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
//            tint = Color.Unspecified,
            tint = AfternoteDesign.colors.iconBk,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentSourceBottomSheetPreview() {
    AfternoteTheme {
        DocumentSourceBottomSheet(
            onPickImage = {},
            onPickFile = {},
        )
    }
}
