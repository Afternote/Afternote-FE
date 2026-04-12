package com.afternote.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    nameState: TextFieldState,
    displayImageUri: String?,
    onEditProfileImageClick: () -> Unit,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.profile_top_bar_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .addFocusCleaner(focusManager)
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(39.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.profile_headline),
                    modifier = Modifier.fillMaxWidth(),
                    style =
                        AfternoteDesign.typography.h1,
                    color = AfternoteDesign.colors.black,
                    textAlign = TextAlign.Start,
                )
                ProfileImage(
                    onClick = onEditProfileImageClick,
                    displayImageUri = displayImageUri,
                )

                AfternoteTextField(
                    state = nameState,
                    placeholder = stringResource(R.string.profile_name_placeholder),
                    imeAction = ImeAction.Done,
                )

                AfternoteButton(
                    text = stringResource(R.string.profile_complete),
                    onClick = {
                        focusManager.clearFocus()
                        onCompleteClick()
                    },
                    type = AfternoteButtonType.Default,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingProfileScreenPreview() {
    AfternoteTheme {
        OnboardingProfileScreen(
            nameState = rememberTextFieldState("Afternote"),
            displayImageUri = null,
            onEditProfileImageClick = {},
            onBackClick = {},
            onCompleteClick = {},
        )
    }
}
