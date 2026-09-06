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
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.presentation.R
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val passkeyCreatedAtFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

private fun formatCreatedAt(raw: String): String =
    runCatching { OffsetDateTime.parse(raw).format(passkeyCreatedAtFormatter) }
        .recoverCatching { LocalDateTime.parse(raw).format(passkeyCreatedAtFormatter) }
        .getOrDefault(raw)

@Composable
internal fun PasskeyListItem(
    passkey: Passkey,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_fingerprint), contentDescription = "패스키")
        Spacer(modifier = Modifier.weight(1f))
        Column {
            Text(passkey.displayName)
            Text(formatCreatedAt(passkey.createdAt))
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
    PasskeyListItem(
        passkey = Passkey(id = 1L, displayName = "아이폰 15 Pro", createdAt = "2026-07-28T10:15:30"),
    )
}
