package com.afternote.feature.setting.presentation.component

import android.R.attr.password
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

@Composable
fun InsertPasswordContent(
    titleText: String,
    passwordLength: Int,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(painterResource(R.drawable.ic_lock), contentDescription = null)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = titleText,
            style = AfternoteDesign.typography.bodyLargeR,
        )
        Spacer(modifier = Modifier.height(32.dp))
        PasswordDots(passwordLength = passwordLength)
        Spacer(modifier = Modifier.weight(1f))
        NumberKeypad(
            onDigitClick = onDigitClick,
            onDeleteClick = onDeleteClick,
            onConfirmClick = onConfirmClick,
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun Prev() {
    InsertPasswordContent(
        titleText = "비밀번호 입력해주세요",
        passwordLength = 4,
        onDigitClick = {},
        onDeleteClick = {},
        onConfirmClick = {},
    )
}
