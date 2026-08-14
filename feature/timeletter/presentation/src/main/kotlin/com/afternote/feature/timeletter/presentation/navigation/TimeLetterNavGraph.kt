package com.afternote.feature.timeletter.presentation.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.timeletter.presentation.screen.sender.DraftLetterScreen
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterDetailScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel

fun NavGraphBuilder.timeLetterNavGraph(
    navController: NavController,
    actions: TimeLetterNavActions,
) {
    navigation<Route.TimeLetter>(startDestination = TimeLetterRoute.TimeLetterHomeRoute) {
        composable<TimeLetterRoute.TimeLetterHomeRoute> {
            TimeletterScreen(
                onSettingClick = actions::onSettingClick,
                onWriteClick = actions::onNavigateToWrite,
                onEditClick = actions::onNavigateToEdit,
                onLetterClick = actions::onNavigateToDetail,
                onFilterRecipientClick = actions::onNavigateToRecipientFilter,
            )
        }

        composable<TimeLetterRoute.TimeLetterWriteRoute> {
            val viewModel: TimeLetterWriteViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.savedAsDraft) {
                if (uiState.savedAsDraft) {
                    viewModel.onSavedAsDraftShown()
                    actions.onDraftBack()
                }
            }
            LaunchedEffect(uiState.registered) {
                if (uiState.registered) {
                    viewModel.onRegisteredShown()
                    actions.onWriteBack()
                }
            }

            TimeLetterWriteScreen(
                uiState = uiState,
                onBackClick = actions::onWriteBack,
                onRegisterClick = { title, textContents -> viewModel.register(title, textContents) },
                onDraftClick = { title, textContents -> viewModel.saveDraft(title, textContents) },
                onNavigateToDraft = actions::onNavigateToDraft,
                onErrorShown = { viewModel.clearError() },
                onRecipientClick = actions::onNavigateToRecipient,
                onDateSelected = { viewModel.setSendAt(it) },
                onTimeSelected = { h, m -> viewModel.setSendTime(h, m) },
                onAddImageBlock = { uri -> viewModel.addImageBlock(uri) },
                onAddAudioBlock = { uri -> viewModel.addAudioBlock(uri) },
                onAddFileBlock = { uri -> viewModel.addFileBlock(uri) },
                onAddLinkBlock = { url -> viewModel.addLinkBlock(url) },
                onRemoveBlock = { id -> viewModel.removeBlock(id) },
                onSetFocusedBlock = { id -> viewModel.setFocusedBlock(id) },
                onAlignCenterClick = { viewModel.setTextAlign(TextAlign.Center) },
                onAlignLeftClick = { viewModel.setTextAlign(TextAlign.Start) },
                onAlignRightClick = { viewModel.setTextAlign(TextAlign.End) },
                onFreePlanLimitConfirm = {
                    // TODO: 구독 화면 및 관련 플로우 구현 시 구독 화면 이동으로 변경
                    viewModel.dismissFreePlanLimitPopup()
                },
                onFreePlanLimitDismiss = { viewModel.dismissFreePlanLimitPopup() },
            )
        }

        composable<TimeLetterRoute.TimeLetterDraftRoute> {
            DraftLetterScreen(onBackClick = actions::onDraftBack)
        }

        composable<TimeLetterRoute.TimeLetterDetailRoute> {
            TimeLetterDetailScreen(
                onBackClick = actions::onDetailBack,
            )
        }

        composable<TimeLetterRoute.TimeLetterRecipientRoute> {
            val writeEntry =
                remember(it) {
                    navController.previousBackStackEntry
                        ?: navController.getBackStackEntry(TimeLetterRoute.TimeLetterWriteRoute())
                }
            val writeViewModel: TimeLetterWriteViewModel = hiltViewModel(writeEntry)
            RecipientListScreen(
                onBackClick = actions::onRecipientBack,
                onConfirmClick = { recipients ->
                    writeViewModel.setRecipients(recipients.map { it.receiverId })
                    actions.onRecipientBack()
                },
            )
        }

        composable<TimeLetterRoute.TimeLetterRecipientFilterRoute> {
            val homeEntry =
                remember(it) {
                    navController.getBackStackEntry(TimeLetterRoute.TimeLetterHomeRoute)
                }
            val timeletterViewModel: TimeletterViewModel = hiltViewModel(homeEntry)
            RecipientListScreen(
                onBackClick = actions::onRecipientFilterBack,
                onConfirmClick = { recipients ->
                    timeletterViewModel.setReceiverFilter(recipients.map { it.receiverId })
                    actions.onRecipientFilterBack()
                },
                allowEmptyConfirm = true,
            )
        }
    }
}
