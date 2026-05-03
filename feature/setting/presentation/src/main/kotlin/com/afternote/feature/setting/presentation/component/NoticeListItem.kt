package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.domain.Notice
import com.afternote.feature.setting.presentation.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val noticeDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

@Composable
fun NoticeListItem(
    notice: Notice,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Text(
                "작성일: ${notice.date.format(noticeDateFormatter)}",
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray6,
            )
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(notice.title, style = AfternoteDesign.typography.bodySmallR)
        }
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painterResource(R.drawable.ic_right_arrow),
            contentDescription = "화살표",
            colorFilter = ColorFilter.tint(AfternoteDesign.colors.gray9),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeListItemPrev() {
    NoticeListItem(
        notice =
            Notice(
                date = LocalDate.of(2022, 11, 20),
                title = "서비스 점검 안내",
                content = "서버 점검으로 인해 서비스가 일시 중단됩니다.",
            ),
    )
}
