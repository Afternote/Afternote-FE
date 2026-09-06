package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.core.ui.R as CoreUiR

@Composable
fun FaqScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.settings_support_faq),
                onBackClick = onBackClick,
            )
        },
        containerColor = AfternoteDesign.colors.gray1,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            item {
                FaqItem(
                    question = stringResource(R.string.faq_default_question),
                    answer = stringResource(R.string.faq_default_answer),
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                )
            }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = question,
                modifier = Modifier.weight(1f),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
            )
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_arrowdown),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f),
                tint = AfternoteDesign.colors.gray7,
            )
        }
        if (expanded) {
            Text(
                text = answer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AfternoteDesign.colors.gray1)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray7,
            )
        }
        HorizontalDivider(color = AfternoteDesign.colors.gray2)
    }
}

@Preview(showBackground = true)
@Composable
private fun FaqScreenPreview() {
    AfternoteTheme {
        FaqScreen(onBackClick = {})
    }
}
