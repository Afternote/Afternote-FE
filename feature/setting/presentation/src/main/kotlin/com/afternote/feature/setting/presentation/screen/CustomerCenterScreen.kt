package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.launch

@Composable
internal fun CustomerCenterScreen(
    onBackClick: () -> Unit,
    onPhoneInquiryClick: () -> Boolean,
    onOneToOneInquiryClick: () -> Unit,
    onEmailInquiryClick: () -> Unit,
    onFaqClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val phoneUnavailableMessage = stringResource(R.string.setting_customer_center_phone_unavailable)
    val emailCopiedMessage = stringResource(R.string.customer_center_email_copied)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.customer_center_title),
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
                BusinessHoursCard(
                    modifier =
                        Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 16.dp),
                )
            }
            item {
                CustomerCenterSectionTitle(text = stringResource(R.string.customer_center_inquiry_channel))
            }
            item {
                CustomerCenterMenuItem(
                    title = stringResource(R.string.customer_center_phone_inquiry),
                    description = stringResource(R.string.customer_center_phone_number),
                    onClick = {
                        if (!onPhoneInquiryClick()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar(phoneUnavailableMessage) }
                        }
                    },
                )
            }
            item {
                CustomerCenterMenuItem(
                    title = stringResource(R.string.customer_center_one_to_one_inquiry),
                    description = stringResource(R.string.customer_center_one_to_one_description),
                    onClick = onOneToOneInquiryClick,
                    enabled = false,
                )
            }
            item {
                CustomerCenterMenuItem(
                    title = stringResource(R.string.customer_center_email_inquiry),
                    description = stringResource(R.string.customer_center_email_address),
                    onClick = {
                        onEmailInquiryClick()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(emailCopiedMessage)
                        }
                    },
                )
            }
            item {
                CustomerCenterSectionTitle(text = stringResource(R.string.customer_center_recipient_section))
            }
            item {
                CustomerCenterMenuItem(
                    title = stringResource(R.string.customer_center_recipient_inquiry),
                    onClick = {},
                    enabled = false,
                )
            }
            item {
                CustomerCenterMenuItem(
                    title = stringResource(R.string.customer_center_faq),
                    onClick = onFaqClick,
                    enabled = false,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.customer_center_device_info_notice),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun BusinessHoursCard(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0.7f to Color(0xFFB7C4CD),
                                    1f to Color(0xFFECF0F3),
                                ),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_telephone),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AfternoteDesign.colors.gray6,
            )
            Text(
                text = stringResource(R.string.customer_center_business_hours_label),
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Text(
            text = stringResource(R.string.customer_center_business_hours),
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray9,
        )
        Text(
            text = stringResource(R.string.customer_center_business_hours_note),
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Composable
private fun CustomerCenterSectionTitle(text: String) {
    Text(
        text = text,
        style = AfternoteDesign.typography.captionLargeB,
        color = AfternoteDesign.colors.gray5,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun CustomerCenterMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(if (description == null) 56.dp else 76.dp)
                    .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray9,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray5,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            RightArrowIcon(
                modifier = Modifier.size(12.dp),
                tint = AfternoteDesign.colors.gray7,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray2)
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerCenterScreenPreview() {
    AfternoteTheme {
        CustomerCenterScreen(
            onBackClick = {},
            onPhoneInquiryClick = { true },
            onOneToOneInquiryClick = {},
            onEmailInquiryClick = {},
            onFaqClick = {},
        )
    }
}
