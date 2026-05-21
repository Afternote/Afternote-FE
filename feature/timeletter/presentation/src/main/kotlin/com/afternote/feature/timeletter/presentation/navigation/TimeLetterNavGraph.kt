package com.afternote.feature.timeletter.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.Route
import com.afternote.feature.timeletter.presentation.screen.sender.DraftLetterScreen
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteEvent
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel

fun NavGraphBuilder.timeLetterNavGraph(
    navController: NavController,
    actions: TimeLetterNavActions,
) {
    navigation<Route.TimeLetter>(startDestination = TimeLetterRoute.TimeLetterHomeRoute) {
        composable<TimeLetterRoute.TimeLetterHomeRoute> {
            TimeletterScreen(
                onWriteClick = actions::onNavigateToWrite,
            )
        }

        composable<TimeLetterRoute.TimeLetterWriteRoute> {
            val viewModel: TimeLetterWriteViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ObserveAsEvents(viewModel.events) { event ->
                when (event) {
                    is TimeLetterWriteEvent.SavedAsDraft -> actions.onDraftBack()
                    is TimeLetterWriteEvent.Registered -> actions.onWriteBack()
                    is TimeLetterWriteEvent.Error -> Unit
                }
            }

            TimeLetterWriteScreen(
                uiState = uiState,
                onBackClick = actions::onWriteBack,
                onRegisterClick = { title, body -> viewModel.register(title, body) },
                onDraftClick = { title, body -> viewModel.saveDraft(title, body) },
                onErrorShown = { viewModel.clearError() },
                onRecipientClick = actions::onNavigateToRecipient,
                onDateSelected = { viewModel.setSendAt(it) },
                onTimeSelected = { h, m -> viewModel.setSendTime(h, m) },
            )
        }

        composable<TimeLetterRoute.TimeLetterDraftRoute> {
            DraftLetterScreen(onBackClick = actions::onDraftBack)
        }

        composable<TimeLetterRoute.TimeLetterRecipientRoute> {
            val writeEntry =
                remember(it) {
                    navController.getBackStackEntry(TimeLetterRoute.TimeLetterWriteRoute)
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
    }
}
