package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.feature.setting.presentation.R

@Composable
fun PasskeyListItem(modifier: Modifier = Modifier) {
    Row {
        Image(painterResource(R.drawable.ic_apple_login), contentDescription = "패스키기본")
        Column {
            Text("이름")
            Text("생성일시")
        }
        Box {
            Image(painterResource(R.drawable.ic_vector1), contentDescription = "왼쪽 막대기")
            Image(painterResource(R.drawable.ic_vector2), contentDescription = "오른쪽 막대기")
        }
    }
}

@Preview
@Composable
private fun PasskeyListItemPrev() {
    PasskeyListItem()
}
