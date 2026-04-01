package com.afternote.feature.afternote.presentation.shared.component.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Black
import com.afternote.core.ui.theme.Gray5
import com.afternote.core.ui.theme.naNumGothic
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.uimodel.AfternoteListDisplayItem

/**
 * Shared list row for 애프터노트 list (writer main and receiver list).
 * White card, icon, serviceName, "최종 작성일 {date}", arrow.
 */
@Composable
fun AfternoteListItem(
    item: AfternoteListDisplayItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable
                { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(item.iconResId),
            contentDescription = item.serviceName,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.serviceName,
                color = Black,
                lineHeight = 24.sp,
                fontSize = 16.sp,
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = stringResource(R.string.afternote_last_written_date, item.date),
                color = Gray5,
                lineHeight = 16.sp,
                fontSize = 10.sp,
                fontFamily = naNumGothic,
                fontWeight = FontWeight.Normal,
            )
        }
        Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.feature_afternote_presentation_right_arrow),
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteListItemPreview() {
    AfternoteTheme {
        AfternoteListItem(
            item =
                AfternoteListDisplayItem(
                    id = "1",
                    serviceName = "인스타그램",
                    date = "2023.11.24",
                    iconResId = R.drawable.img_insta_pattern,
                ),
        )
    }
}
