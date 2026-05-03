package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.feature.setting.presentation.R

@Composable
fun PasskeyListItem(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_apple_login), contentDescription = "패스키기본")
        Spacer(modifier = Modifier.weight(1f))
        Column {
            Text("이름")
            Text("생성일시")
        }
        Spacer(modifier = Modifier.weight(1f))
        Box {
            Image(painterResource(R.drawable.ic_vector1), contentDescription = "왼쪽 막대기")
            Image(painterResource(R.drawable.ic_vector2), contentDescription = "오른쪽 막대기")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PasskeyListItemPrev() {
    PasskeyListItem()
}
