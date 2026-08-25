package com.afternote.core.ui.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    showProfileIcon: Boolean = true,
    onSettingClick: () -> Unit = {},
) {
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
                if (showProfileIcon) {
                    Image(
                        painter = painterResource(R.drawable.core_ui_user),
                        contentDescription = stringResource(R.string.core_ui_home_top_bar_profile),
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(modifier = Modifier.width(15.dp))
                }

                Image(
                    painter = painterResource(R.drawable.core_ui_settings),
                    // 유일하게 눌리는 액션인데 접근성 트리에 이름이 없었다 — 스크린리더가
                    // "버튼" 으로만 읽는다 (#613).
                    contentDescription = stringResource(R.string.core_ui_home_top_bar_setting),
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button) { onSettingClick() },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    HomeTopBar()
}
