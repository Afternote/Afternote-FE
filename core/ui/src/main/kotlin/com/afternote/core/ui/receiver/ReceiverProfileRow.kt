package com.afternote.core.ui.receiver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 수신자 한 명을 프로필 이미지 + 이름 + 관계로 그리는 행 뼈대.
 *
 * [trailing] 만 소비자가 채운다 — 선택 화면([ReceiverSelectScreen])은 체크박스를,
 * 설정 관리 화면(feature:setting `ReceiverManageScreen`, #631)은 이동 화살표를 넣는다.
 * 행 전체가 [onClick] 을 갖는 클릭 영역이다.
 */
@Composable
fun ReceiverProfileRow(
    name: String,
    relation: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileImage(size = 50.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AfternoteDesign.typography.captionLargeB,
            )
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = relation,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray8,
            )
        }
        trailing()
    }
}
