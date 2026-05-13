package com.afternote.feature.timeletter.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.timeletter.presentation.screen.sender.DraftLetterScreen
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

fun NavGraphBuilder.timeLetterNavGraph(actions: TimeLetterNavActions) {
    navigation<Route.TimeLetter>(startDestination = TimeLetterRoute.TimeLetterHomeRoute) {
        composable<TimeLetterRoute.TimeLetterHomeRoute> {
            TimeletterScreen(
                onWriteClick = actions::onNavigateToWrite,
            )
        }

        composable<TimeLetterRoute.TimeLetterWriteRoute> {
            TimeLetterWriteScreen(
                onBackClick = actions::onWriteBack,
                onRecipientClick = actions::onNavigateToRecipient,
                onDraftClick = actions::onNavigateToDraft,
            )
        }

        composable<TimeLetterRoute.TimeLetterDraftRoute> {
            DraftLetterScreen(onBackClick = actions::onDraftBack)
        }

        composable<TimeLetterRoute.TimeLetterRecipientRoute> {
            RecipientListScreen(
                onBackClick = actions::onRecipientBack,
                onConfirmClick = { actions.onRecipientBack() },
            )
        }
    }
}
