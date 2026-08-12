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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun CustomerCenterScreen(
    onBackClick: () -> Unit,
    onPhoneInquiryClick: () -> Unit,
    onOneToOneInquiryClick: () -> Unit,
    onEmailInquiryClick: () -> Unit,
    onRecipientInquiryClick: () -> Unit,
    onFaqClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.customer_center_title),
                onBackClick = onBackClick,
            )
        },
        containerColor = AfternoteDesign.colors.gray1,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            BusinessHoursCard(
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 16.dp),
            )

            CustomerCenterSectionTitle(text = stringResource(R.string.customer_center_inquiry_channel))
            CustomerCenterMenuItem(
                title = stringResource(R.string.customer_center_phone_inquiry),
                description = stringResource(R.string.customer_center_phone_number),
                onClick = onPhoneInquiryClick,
            )
            CustomerCenterMenuItem(
                title = stringResource(R.string.customer_center_one_to_one_inquiry),
                description = stringResource(R.string.customer_center_one_to_one_description),
                onClick = onOneToOneInquiryClick,
            )
            CustomerCenterMenuItem(
                title = stringResource(R.string.customer_center_email_inquiry),
                description = stringResource(R.string.customer_center_email_address),
                onClick = onEmailInquiryClick,
            )

            CustomerCenterSectionTitle(text = stringResource(R.string.customer_center_recipient_section))
            CustomerCenterMenuItem(
                title = stringResource(R.string.customer_center_recipient_inquiry),
                onClick = onRecipientInquiryClick,
            )
            CustomerCenterMenuItem(
                title = stringResource(R.string.customer_center_faq),
                onClick = onFaqClick,
            )

            Text(
                text = stringResource(R.string.customer_center_device_info_notice),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )
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
                    color = AfternoteDesign.colors.gray2,
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
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white)
                .clickable(onClick = onClick),
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
                modifier = Modifier.size(24.dp),
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
            onPhoneInquiryClick = {},
            onOneToOneInquiryClick = {},
            onEmailInquiryClick = {},
            onRecipientInquiryClick = {},
            onFaqClick = {},
        )
    }
}
