package com.afternote.feature.setting.presentation.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.model.InquiryStatus
import com.afternote.feature.setting.presentation.model.InquiryUiModel
import com.afternote.feature.setting.presentation.model.sampleInquiries

@Composable
fun InquiryListScreen(
    inquiries: List<InquiryUiModel>,
    onBackClick: () -> Unit,
    onInquiryClick: (Long) -> Unit,
    onNewInquiryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { InquiryTopBar(onBackClick) },
        bottomBar = {
            AfternoteButton(
                text = stringResource(R.string.inquiry_new),
                onClick = onNewInquiryClick,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            items(inquiries, key = { it.id }) { inquiry ->
                InquiryListItem(inquiry, onClick = { onInquiryClick(inquiry.id) })
            }
        }
    }
}

@Composable
private fun InquiryListItem(
    inquiry: InquiryUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = AfternoteDesign.colors.white,
        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (inquiry.status == InquiryStatus.ANSWERED) Color(0xFFDCEDE3) else AfternoteDesign.colors.gray2,
                ) {
                    Text(
                        text =
                            stringResource(
                                if (inquiry.status ==
                                    InquiryStatus.ANSWERED
                                ) {
                                    R.string.inquiry_answered
                                } else {
                                    R.string.inquiry_received
                                },
                            ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = AfternoteDesign.typography.captionLargeB,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(inquiry.date, style = AfternoteDesign.typography.footnoteCaption, color = AfternoteDesign.colors.gray7)
            }
            Spacer(Modifier.height(12.dp))
            Text(inquiry.title, style = AfternoteDesign.typography.bodyBase, color = AfternoteDesign.colors.gray9)
            Spacer(Modifier.height(6.dp))
            Text(
                text = inquiry.content.replace('\n', ' '),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Composable
fun InquiryDetailScreen(
    inquiry: InquiryUiModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { InquiryTopBar(onBackClick) },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(inquiry.date, style = AfternoteDesign.typography.footnoteCaption, color = AfternoteDesign.colors.gray6)
                Spacer(Modifier.height(10.dp))
                Text(inquiry.title, style = AfternoteDesign.typography.h2, color = AfternoteDesign.colors.gray9)
            }
            HorizontalDivider(color = AfternoteDesign.colors.gray2)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(inquiry.content, style = AfternoteDesign.typography.bodySmallR, color = AfternoteDesign.colors.gray9)
                inquiry.answer?.let { answer ->
                    HorizontalDivider(Modifier.padding(vertical = 28.dp), color = AfternoteDesign.colors.gray3)
                    Text(
                        stringResource(R.string.inquiry_answer),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AfternoteDesign.colors.white,
                        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
                    ) {
                        Text(
                            answer,
                            Modifier.padding(24.dp),
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InquiryWriteScreen(
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var type by rememberSaveable { mutableStateOf("\ud0c0\uc784\ub808\ud130") }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val onAddScreenshots: () -> Unit =
        if (LocalInspectionMode.current) {
            ({})
        } else {
            val picker =
                rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
                    attachments = (attachments + uris).distinct().take(MAX_INQUIRY_IMAGES)
                }
            ({ picker.launch("image/*") })
        }
    val canSubmit = title.isNotBlank() && content.isNotBlank()

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = { InquiryTopBar(onBackClick) },
        bottomBar = {
            AfternoteButton(
                text = stringResource(R.string.inquiry_submit),
                onClick = onSubmitClick,
                type = if (canSubmit) AfternoteButtonType.Default else AfternoteButtonType.Un,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            InquiryTypeDropdown(type, onSelected = { type = it })
            Spacer(Modifier.height(22.dp))
            InquiryFieldLabel(stringResource(R.string.inquiry_title))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.inquiry_text_field)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(18.dp))
            InquiryFieldLabel(stringResource(R.string.inquiry_content))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text(stringResource(R.string.inquiry_text_field)) },
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = attachments.size < MAX_INQUIRY_IMAGES,
                            onClick = onAddScreenshots,
                        ),
                shape = RoundedCornerShape(8.dp),
                color = AfternoteDesign.colors.white,
                border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_image),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = AfternoteDesign.colors.gray7,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.inquiry_add_screenshot, attachments.size, MAX_INQUIRY_IMAGES),
                        style = AfternoteDesign.typography.bodySmallR,
                    )
                }
            }
        }
    }
}

@Composable
private fun InquiryTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    InquiryFieldLabel(stringResource(R.string.inquiry_type))
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selected, Modifier.weight(1f), style = AfternoteDesign.typography.bodySmallR)
            Text("\u2304", style = AfternoteDesign.typography.bodyLargeR)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "\ud0c0\uc784\ub808\ud130",
                "\ub9c8\uc74c \uae30\ub85d",
                "\uc560\ud504\ud130\ub178\ud2b8",
                "\uae30\ud0c0",
            ).forEach { item ->
                DropdownMenuItem(text = { Text(item) }, onClick = {
                    onSelected(item)
                    expanded = false
                })
            }
        }
    }
    HorizontalDivider(color = AfternoteDesign.colors.gray2)
}

@Composable
private fun InquiryFieldLabel(text: String) {
    Text(text, style = AfternoteDesign.typography.bodySmallR, color = AfternoteDesign.colors.gray7)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InquiryTopBar(onBackClick: () -> Unit) {
    DetailTopBar(stringResource(R.string.inquiry_title_bar), onBackClick = onBackClick)
}

@Preview(name = "Inquiry list", showBackground = true)
@Composable
private fun InquiryListScreenPreview() {
    AfternoteTheme {
        InquiryListScreen(
            inquiries = sampleInquiries,
            onBackClick = {},
            onInquiryClick = {},
            onNewInquiryClick = {},
        )
    }
}

@Preview(name = "Inquiry detail - answered", showBackground = true)
@Composable
private fun InquiryDetailScreenPreview() {
    AfternoteTheme {
        InquiryDetailScreen(
            inquiry = sampleInquiries.first(),
            onBackClick = {},
        )
    }
}

@Preview(name = "Inquiry write", showBackground = true)
@Composable
private fun InquiryWriteScreenPreview() {
    AfternoteTheme {
        InquiryWriteScreen(
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}

private const val MAX_INQUIRY_IMAGES = 3
