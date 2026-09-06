package com.afternote.feature.setting.presentation.screen

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.icon.CloseIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.model.InquiryStatus
import com.afternote.feature.setting.presentation.model.InquiryUiModel
import kotlinx.coroutines.launch

@Composable
internal fun InquiryListScreen(
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
                text = stringResource(R.string.setting_inquiry_new),
                onClick = onNewInquiryClick,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        if (inquiries.isEmpty()) {
            InquiryEmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
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
}

@Composable
private fun InquiryEmptyState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.setting_inquiry_list_empty),
) {
    Box(
        modifier = modifier.padding(horizontal = 20.dp).padding(top = 24.dp),
    ) {
        Text(
            text = message,
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
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
                                    R.string.setting_inquiry_answered
                                } else {
                                    R.string.setting_inquiry_received
                                },
                            ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = AfternoteDesign.typography.captionLargeB,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(inquiry.date, style = AfternoteDesign.typography.footnoteCaption, color = AfternoteDesign.colors.gray9)
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
internal fun InquiryDetailScreen(
    inquiry: InquiryUiModel?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { InquiryTopBar(onBackClick) },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        if (inquiry == null) {
            InquiryEmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                message = stringResource(R.string.setting_inquiry_detail_not_found),
            )
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(inquiry.date, style = AfternoteDesign.typography.footnoteCaption, color = AfternoteDesign.colors.gray8)
                Spacer(Modifier.height(10.dp))
                Text(inquiry.title, style = AfternoteDesign.typography.h2, color = AfternoteDesign.colors.gray9)
            }
            HorizontalDivider(color = AfternoteDesign.colors.gray2)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(inquiry.content, style = AfternoteDesign.typography.bodySmallR, color = AfternoteDesign.colors.gray9)
                inquiry.answer?.let { answer ->
                    HorizontalDivider(Modifier.padding(vertical = 28.dp), color = AfternoteDesign.colors.gray3)
                    Text(
                        stringResource(R.string.setting_inquiry_answer),
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
internal fun InquiryWriteScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inquiryTypes = stringArrayResource(R.array.setting_inquiry_types).toList()
    var type by rememberSaveable { mutableStateOf(inquiryTypes.first()) }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var attachments by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val submitNotSupportedMessage = stringResource(R.string.setting_inquiry_submit_not_supported)
    val attachmentLimitMessage = stringResource(R.string.setting_inquiry_attachment_limit, MAX_INQUIRY_IMAGES)
    val attachmentUnavailableMessage = stringResource(R.string.setting_inquiry_attachment_unavailable)
    val onAddScreenshots: () -> Unit =
        if (LocalInspectionMode.current) {
            ({})
        } else {
            val picker =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                    val selected = (attachments + uris.map(Uri::toString)).distinct()
                    val candidates = selected.take(MAX_INQUIRY_IMAGES)
                    val readableUris =
                        candidates.filter { value ->
                            val uri = Uri.parse(value)
                            try {
                                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                true
                            } catch (_: SecurityException) {
                                false
                            }
                        }
                    attachments = ArrayList(readableUris)
                    val message =
                        when {
                            readableUris.size < candidates.size -> attachmentUnavailableMessage
                            selected.size > MAX_INQUIRY_IMAGES -> attachmentLimitMessage
                            else -> null
                        }
                    if (message != null) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
            ({ picker.launch(arrayOf("image/*")) })
        }
    val canSubmit = title.isNotBlank() && content.isNotBlank()

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = { InquiryTopBar(onBackClick) },
        bottomBar = {
            AfternoteButton(
                text = stringResource(R.string.setting_inquiry_submit),
                onClick = {
                    // Afternote-BE#246: 접수 계약이 생길 때까지 작성 내용을 남기고 미지원을 알린다.
                    coroutineScope.launch { snackbarHostState.showSnackbar(submitNotSupportedMessage) }
                },
                type = if (canSubmit) AfternoteButtonType.Default else AfternoteButtonType.Un,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AfternoteDesign.colors.gray1,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            InquiryTypeDropdown(type, options = inquiryTypes, onSelected = { type = it })
            Spacer(Modifier.height(22.dp))
            InquiryFieldLabel(stringResource(R.string.setting_inquiry_title))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.setting_inquiry_title_text_field),
                        color = AfternoteDesign.colors.gray4,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Spacer(Modifier.height(18.dp))
            InquiryFieldLabel(stringResource(R.string.setting_inquiry_content))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.setting_inquiry_content_text_field),
                        color = AfternoteDesign.colors.gray4,
                    )
                },
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
                        stringResource(R.string.setting_inquiry_add_screenshot, attachments.size, MAX_INQUIRY_IMAGES),
                        style = AfternoteDesign.typography.bodySmallR,
                    )
                }
            }
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEachIndexed { index, uri ->
                        Box(Modifier.weight(1f).aspectRatio(1f)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = stringResource(R.string.setting_inquiry_attachment, index + 1),
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            IconButton(
                                onClick = { attachments = ArrayList(attachments.filterNot { it == uri }) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                CloseIcon(
                                    contentDescription = stringResource(R.string.setting_inquiry_remove_attachment, index + 1),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    repeat(MAX_INQUIRY_IMAGES - attachments.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun InquiryTypeDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    InquiryFieldLabel(stringResource(R.string.setting_inquiry_type))
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selected, Modifier.weight(1f), style = AfternoteDesign.typography.bodySmallR)
            Icon(
                painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_arrowdown),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AfternoteDesign.colors.gray7,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
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
    DetailTopBar(stringResource(R.string.setting_inquiry_title_bar), onBackClick = onBackClick)
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

@Preview(name = "Inquiry list - empty", showBackground = true)
@Composable
private fun InquiryListScreenEmptyPreview() {
    AfternoteTheme {
        InquiryListScreen(
            inquiries = emptyList(),
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

// 프리뷰 전용 표본 데이터 — 프로덕션 라우트(SettingNavGraph)에서는 사용하지 않는다.
private val sampleInquiries =
    listOf(
        InquiryUiModel(
            id = 1L,
            status = InquiryStatus.ANSWERED,
            date = "2025.08.09.",
            title = "타임레터 발송일 변경이 안 돼요",
            content =
                "타임레터를 작성한 뒤 발송일을 변경하려고 하는데 날짜를 선택해도 기존 날짜로 계속 표시됩니다. " +
                    "앱을 종료했다가 다시 실행해도 동일하고, 수정 버튼을 눌러 저장해도 변경사항이 반영되지 않아요.\n" +
                    "현재 발송 예정일은 8월 20일로 설정되어 있고, 9월 15일로 변경하려고 합니다. 확인 부탁드립니다.",
            answer =
                "안녕하세요 애프터노트입니다.\n\n타임레터 발신인은 마이페이지에서 변경하실 수 있습니다.\n" +
                    "마이페이지 > 타임레터 관리에서 변경을 원하시는 타임레터를 선택한 후, 발송예정일을 변경해 주세요.\n" +
                    "변경된 날짜는 저장 후 정상적으로 반영됩니다.\n\n" +
                    "추가로 이용에 어려움이 있으실 경우 언제든지 문의해 주세요.\n\n감사합니다.",
        ),
        InquiryUiModel(
            id = 2L,
            status = InquiryStatus.RECEIVED,
            date = "2025.08.09.",
            title = "타임레터 발송일 변경이 안 돼요",
            content = "발송 예약일은 마이페이지에서 변경이 가능한지 문의드립니다.",
            answer = null,
        ),
    )

@Preview(name = "Inquiry write", showBackground = true)
@Composable
private fun InquiryWriteScreenPreview() {
    AfternoteTheme {
        InquiryWriteScreen(
            onBackClick = {},
        )
    }
}

private const val MAX_INQUIRY_IMAGES = 3
