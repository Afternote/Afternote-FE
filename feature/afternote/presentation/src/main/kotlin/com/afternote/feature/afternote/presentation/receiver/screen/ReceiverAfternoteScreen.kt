package com.afternote.feature.afternote.presentation.receiver.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.button.ClickButton
import com.afternote.core.ui.feedback.ConfirmationPopup
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.nanumGothic
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.component.ContentSection
import com.afternote.feature.afternote.presentation.receiver.component.HeroCard
import com.afternote.feature.afternote.presentation.receiver.component.TopHeader
import com.afternote.feature.afternote.presentation.receiver.model.uistate.ReceiverDownloadAllUiState
import com.afternote.feature.afternote.presentation.receiver.model.uistate.ReceiverSummaryUiState
import com.afternote.feature.afternote.presentation.receiver.viewmodel.ReceiverDownloadAllEvent
import com.afternote.feature.afternote.presentation.receiver.viewmodel.ReceiverDownloadAllViewModel
import com.afternote.feature.afternote.presentation.receiver.viewmodel.ReceiverDownloadAllViewModelContract

/**
 * 수신자 애프터노트 메인 Entry.
 *
 * ViewModel에서 데이터를 로드·가공하고, Entry는 Screen에 전달만 합니다.
 */
@Composable
fun ReceiverAfterNoteEntry(
    modifier: Modifier = Modifier,
    summary: ReceiverSummaryUiState = ReceiverSummaryUiState(),
    showBottomBar: Boolean = true,
    selectedNavTab: BottomNavTab = BottomNavTab.NOTE,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onNavigateToRecord: () -> Unit = {},
    onNavigateToTimeLetter: () -> Unit = {},
    onNavigateToAfternote: () -> Unit = {},
    viewModel: ReceiverDownloadAllViewModelContract = hiltViewModel<ReceiverDownloadAllViewModel>(),
) {
    val downloadUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(downloadUiState.downloadSuccess) {
        if (downloadUiState.downloadSuccess) {
            viewModel.onEvent(ReceiverDownloadAllEvent.DownloadSuccessConsumed)
        }
    }
    LaunchedEffect(downloadUiState.errorMessage) {
        downloadUiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ReceiverDownloadAllEvent.ErrorConsumed)
        }
    }

    ReceiverAfterNoteScreen(
        modifier = modifier,
        summary = summary,
        downloadUiState = downloadUiState,
        showBottomBar = showBottomBar,
        selectedNavTab = selectedNavTab,
        onNavTabSelected = onNavTabSelected,
        onNavigateToRecord = onNavigateToRecord,
        onNavigateToTimeLetter = onNavigateToTimeLetter,
        onNavigateToAfternote = onNavigateToAfternote,
        onDownloadConfirm = { viewModel.onEvent(ReceiverDownloadAllEvent.ConfirmDownload(summary.authCode)) },
    )
}

/**
 * 수신자 애프터노트 메인 Screen.
 *
 * ViewModel 의존성 없이 순수하게 UI만 그립니다. Preview 가능합니다.
 */
@Composable
fun ReceiverAfterNoteScreen(
    modifier: Modifier = Modifier,
    summary: ReceiverSummaryUiState = ReceiverSummaryUiState(),
    downloadUiState: ReceiverDownloadAllUiState = ReceiverDownloadAllUiState(),
    showBottomBar: Boolean = true,
    selectedNavTab: BottomNavTab = BottomNavTab.NOTE,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    onNavigateToRecord: () -> Unit = {},
    onNavigateToTimeLetter: () -> Unit = {},
    onNavigateToAfternote: () -> Unit = {},
    onDownloadConfirm: () -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ConfirmationPopup(
            message = stringResource(R.string.receiver_download_all_dialog_message),
            onDismiss = { showDialog = false },
            onConfirm = {
                onDownloadConfirm()
                showDialog = false
            },
            isLoading = downloadUiState.isLoading,
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = { TopHeader() },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    selectedNavTab = selectedNavTab,
                    onTabClick = onNavTabSelected,
                )
            }
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(scrollState),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.receiver_sender_record_title,
                        summary.senderName.ifBlank { "" },
                    ),
                fontWeight = FontWeight.Bold,
                color = AfternoteDesign.colors.gray9,
                fontFamily = nanumGothic,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            HeroCard(
                leaveMessage =
                    summary.leaveMessage?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.receiver_hero_default_message),
            )

            Spacer(modifier = Modifier.height(24.dp))

            ContentSection(
                title = stringResource(R.string.receiver_mindrecord_section_title),
                desc = stringResource(R.string.receiver_mindrecord_section_desc),
                subDesc =
                    stringResource(
                        R.string.receiver_mindrecord_section_count,
                        summary.mindRecordTotalCount,
                    ),
                btnText = stringResource(R.string.receiver_mindrecord_section_button),
                imageResource = painterResource(R.drawable.img_book),
                onButtonClick = onNavigateToRecord,
            )

            ContentSection(
                title = stringResource(R.string.receiver_timeletter_section_title),
                desc = stringResource(R.string.receiver_timeletter_section_desc),
                subDesc =
                    stringResource(
                        R.string.receiver_timeletter_section_count,
                        summary.timeLetterTotalCount,
                    ),
                btnText = stringResource(R.string.receiver_timeletter_section_button),
                imageResource = painterResource(R.drawable.img_letter),
                onButtonClick = onNavigateToTimeLetter,
            )

            ContentSection(
                title = stringResource(R.string.receiver_afternote_section_title),
                desc = stringResource(R.string.receiver_afternote_section_desc),
                subDesc =
                    stringResource(
                        R.string.receiver_afternote_section_count,
                        summary.afternoteTotalCount,
                    ),
                btnText = stringResource(R.string.receiver_afternote_section_button),
                imageResource = painterResource(R.drawable.img_notebook),
                onButtonClick = onNavigateToAfternote,
            )

            Spacer(modifier = Modifier.height(20.dp))

            ClickButton(
                color = AfternoteDesign.colors.gray9,
                onButtonClick = { showDialog = true },
                title = stringResource(R.string.receiver_download_all_button),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewReceiverAfterNote() {
    MaterialTheme {
        ReceiverAfterNoteScreen(
            summary =
                ReceiverSummaryUiState(
                    senderName = "홍길동",
                    mindRecordTotalCount = 3,
                    timeLetterTotalCount = 2,
                    afternoteTotalCount = 5,
                ),
        )
    }
}
