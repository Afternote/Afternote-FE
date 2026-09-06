package com.afternote.core.ui.sheet

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * [MediaSelectSheetContent] 한 줄에 실리는 항목.
 *
 * 항목 수를 계약에 박지 않고 리스트로 받는 이유는 화면마다 갈래가 다르기 때문이다 —
 * 미디어 추가 4갈래(이미지·음성·파일·링크), 서류 업로드 2갈래(이미지·파일),
 * 추억 노트 미디어 2~3갈래(갤러리·촬영·삭제). 갈래가 늘어도 이 타입은 그대로다.
 *
 * @param iconTint 아이콘 색. [Color.Unspecified] 면 기본값(`iconBk`)을 쓴다.
 * @param labelColor 문구 색. [Color.Unspecified] 면 기본값(`gray9`)을 쓴다.
 *   되돌릴 수 없는 파괴적 항목("삭제")은 호출부가 `error` 색을 실어 다른 갈래와 가른다 — 같은
 *   회색이면 실수 탭을 유도한다.
 */
@Immutable
data class MediaSheetItem(
    @param:DrawableRes val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
    val iconTint: Color = Color.Unspecified,
    val labelColor: Color = Color.Unspecified,
)

/**
 * "미디어 추가하기" 계열 메뉴 시트의 본문 — 드래그 핸들 + 헤더 + 항목 목록.
 *
 * 정본 시안 [4327:72281](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-72281)
 * (`정리 Screen Design` 의 공용 media_select 컴포넌트). 시안 실측값:
 * - 좌우 여백 20dp (390 폭 안에 350 폭 컨테이너)
 * - 핸들 40×4, 시트 상단에서 8dp
 * - 핸들 아래 14dp → 헤더 문구(bodyBase 16/24, gray6) → 아래 18dp
 * - 각 행 56dp = 패딩 16dp + 아이콘 24dp + 패딩 16dp, 아이콘–문구 간격 10dp
 * - 행마다 아래쪽 1dp gray3 디바이더(마지막 행 포함, 첫 행 위에는 없음)
 *
 * `ModalBottomSheet` 를 직접 들고 있는 호출부(자체 `sheetState`·높이 계산이 필요한 화면)가
 * 본문만 가져다 쓸 수 있도록 래퍼([MediaSelectBottomSheet])와 분리해 둔다. 드래그 핸들은
 * 본문 안에 있으므로 감싸는 `ModalBottomSheet` 는 `dragHandle = null` 로 둔다.
 *
 * @param bottomSpacing 마지막 디바이더 아래 여백. 시트 높이를 호출부가 고정하는 경우엔
 *   보이지 않으므로 기본값을 그대로 둬도 된다.
 */
@Composable
fun MediaSelectSheetContent(
    items: List<MediaSheetItem>,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.core_ui_media_sheet_title),
    bottomSpacing: Dp = 32.dp,
) {
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
                    // 근사 토큰: 시안 원본 #CCCCCC → 기존 토큰 중 gray3(#E0E0E0).
                    // `BottomSheetCalendar` 핸들이 잡아 둔 선례를 따른다(원시 hex 를 새로 들이지 않는다).
                    .background(AfternoteDesign.colors.gray3),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.height(18.dp))
        items.forEach { item ->
            MediaSheetRow(item = item)
        }
        Spacer(Modifier.height(bottomSpacing))
    }
}

/**
 * [MediaSelectSheetContent] 를 `ModalBottomSheet` 로 감싼 판 — 시트 자체를 따로 조립할 일이
 * 없는 호출부용.
 *
 * @param modifier `ModalBottomSheet` 에 걸린다. 시트 높이를 지정해야 하면 여기에 실는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSelectBottomSheet(
    onDismiss: () -> Unit,
    items: List<MediaSheetItem>,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.core_ui_media_sheet_title),
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = AfternoteDesign.colors.gray1,
        // 핸들은 본문(MediaSelectSheetContent)이 시안 위치대로 직접 그린다.
        dragHandle = null,
    ) {
        MediaSelectSheetContent(
            items = items,
            title = title,
        )
    }
}

@Composable
private fun MediaSheetRow(item: MediaSheetItem) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = item.iconTint.takeOrElse { AfternoteDesign.colors.iconBk },
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.label,
            style = AfternoteDesign.typography.bodyBase,
            color = item.labelColor.takeOrElse { AfternoteDesign.colors.gray9 },
        )
    }
}
