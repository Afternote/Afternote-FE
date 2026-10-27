package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.MainViewModel
import com.afternote.afternote_fe.R
import com.afternote.core.ui.Route
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import kotlinx.coroutines.launch

/**
 * 앱 진입 보고(#2321)가 돌려준 것을 사용자에게 한 번 보여 준다.
 *
 * - 보관 확인 절차가 취소됐으면 그 사실을 스낵바로 알린다. 앱을 연 것만으로 취소되는 절차라, 사용자가
 *   따로 할 일은 없다.
 * - 오래 비웠다 돌아왔으면 쉬운 질문 셋을 시트로 띄운다. 고르면 그 질문을 제목으로 일기를 연다.
 *
 * 알림 진입이 큐에 남아 있는 동안은 띄우지 않는다 — 알림을 눌러 들어온 사용자는 그 목적지가 먼저다.
 * 홈 바텀바 스택에 올라선 뒤에만 띄워, 온보딩 위에 시트가 뜨지 않게 한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryNoticeEffect(
    appState: AppState,
    mainViewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val notice by mainViewModel.entryNotice.collectAsStateWithLifecycle()
    val pendingNotificationEntry by mainViewModel.pendingNotificationEntry.collectAsStateWithLifecycle()
    val authenticatedStartRoute by mainViewModel.startRoute.collectAsStateWithLifecycle()
    val isReady =
        authenticatedStartRoute == Route.Home && appState.activeStack != AppStack.FLOW && pendingNotificationEntry == null
    val current = notice?.takeIf { isReady } ?: return

    val cancelledMessage = stringResource(R.string.entry_notice_procedure_cancelled)
    val snackbarScope = rememberCoroutineScope()
    LaunchedEffect(current) {
        if (current.procedureCancelled) snackbarScope.launch { snackbarHostState.showSnackbar(cancelledMessage) }
        if (current.easyQuestions.isEmpty()) mainViewModel.consumeEntryNotice()
    }
    if (current.easyQuestions.isEmpty()) return

    ModalBottomSheet(
        onDismissRequest = mainViewModel::consumeEntryNotice,
        containerColor = AfternoteDesign.colors.gray1,
    ) {
        EasyQuestionsContent(
            questions = current.easyQuestions,
            onQuestionClick = { question ->
                mainViewModel.consumeEntryNotice()
                appState.navigateToMindRecordDestination(MindRecordRoute.DiaryWriteRoute(prefillTitle = question))
            },
            onDismiss = mainViewModel::consumeEntryNotice,
        )
    }
}

@Composable
internal fun EasyQuestionsContent(
    questions: List<String>,
    onQuestionClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.entry_notice_easy_questions_title),
            style = AfternoteDesign.typography.bodyLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.entry_notice_easy_questions_subtitle),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(Modifier.height(12.dp))
        questions.forEach { question ->
            Text(
                text = question,
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onQuestionClick(question) }
                        .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                        .padding(vertical = 16.dp),
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.entry_notice_easy_questions_later),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}
