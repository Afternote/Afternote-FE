package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.SocialAccountState

@Composable
fun SocialAccountRow(
    account: SocialAccountState,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(account.iconRes),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(account.labelRes),
                style = AfternoteDesign.typography.bodyBase,
            )
            Spacer(modifier = Modifier.padding(top = 10.dp))
            Text(
                text = account.email ?: stringResource(R.string.status_not_connected),
                style = AfternoteDesign.typography.captionLargeR,
                color =
                    if (account.isConnected) {
                        AfternoteDesign.colors.gray5
                    } else {
                        AfternoteDesign.colors.b1
                    },
            )
        }

        Switch(
            checked = account.isConnected,
            onCheckedChange = onToggle,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RowPrev() {
    SocialAccountRow(
        account =
            SocialAccountState(
                iconRes = R.drawable.ic_google_logo,
                labelRes = R.string.login_with_google,
                isConnected = true,
                email = "example@gmail.com",
            ),
        onToggle = {},
    )
}
