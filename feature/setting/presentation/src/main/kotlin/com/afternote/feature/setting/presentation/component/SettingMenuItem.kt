package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.feature.setting.presentation.R

@Composable
fun SettingMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painterResource(R.drawable.ic_right_arrow),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingMenuItemPrev() {
    SettingMenuItem(label = "프로필 수정", onClick = {})
}
