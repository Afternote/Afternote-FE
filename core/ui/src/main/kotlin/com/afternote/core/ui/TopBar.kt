package com.afternote.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(R.drawable.core_ui_logo),
                contentDescription = null,
                modifier = Modifier.size(90.dp)
            )
        },
        title = { },
        actions = {
            Row {
                Image(
                    painter = painterResource(R.drawable.core_ui_user),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(15.dp))

                Image(
                    painter = painterResource(R.drawable.core_ui_settings),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun TopBarPreview() {
    TopBar()
}
