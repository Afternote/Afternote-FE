package com.afternote.core.ui.scaffold.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(com.afternote.core.common.R.drawable.core_common_logo),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(start = 25.dp)
                        .size(90.dp),
            )
        },
        title = { },
        actions = {
            Row(
                modifier =
                    Modifier
                        .padding(end = 25.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.core_ui_user),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(15.dp))

                Image(
                    painter = painterResource(R.drawable.core_ui_settings),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = AfternoteDesign.colors.gray2,
            ),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    HomeTopBar()
}
