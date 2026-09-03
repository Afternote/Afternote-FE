package com.afternote.feature.onboarding.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.ProfileImagePicker
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.onboarding.presentation.signup.SignUpIntent
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingProfileContent(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // VM 은 Android Framework 의존 제거를 위해 String 으로 보관한다. Uri 변환은 UI 층 몫이다.
    val displayImageUri = state.profileImageUri?.toUri()
    val nameState = rememberTextFieldState(state.name)

    LaunchedEffect(nameState) {
        snapshotFlow { nameState.text.toString() }.collect { onIntent(SignUpIntent.UpdateName(it)) }
    }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = PickVisualMedia(),
            onResult = { uri ->
                handleProfileImagePickerResult(uri) { picked -> onIntent(SignUpIntent.PickProfileImage(picked.toString())) }
            },
        )

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.onboarding_profile_top_bar_title),
                onBackClick = {
                    focusManager.clearFocus()
                    onBackClick()
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    text = stringResource(R.string.onboarding_profile_headline),
                    modifier = Modifier.fillMaxWidth(),
                    style = AfternoteDesign.typography.h1,
                    color = AfternoteDesign.colors.black,
                    textAlign = TextAlign.Start,
                )
                ProfileImagePicker(
                    onPickClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly),
                        )
                    },
                    displayImageUri = displayImageUri?.toString(),
                )

                val isNameProvided =
                    nameState.text
                        .toString()
                        .trim()
                        .isNotEmpty()

                // 제출 중에는 버튼과 IME 두 경로 모두 잠근다. 한쪽만 막으면 다른 쪽으로 중복 제출된다.
                val isCompleteEnabled = isNameProvided && !state.isLoading

                AfternoteTextField(
                    state = nameState,
                    placeholder = stringResource(R.string.onboarding_profile_name_placeholder),
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        if (isCompleteEnabled) onIntent(SignUpIntent.SubmitSignUp)
                    },
                )

                AfternoteButton(
                    text = stringResource(R.string.onboarding_profile_complete),
                    onClick = {
                        focusManager.clearFocus()
                        if (isCompleteEnabled) onIntent(SignUpIntent.SubmitSignUp)
                    },
                    type = if (isCompleteEnabled) AfternoteButtonType.Default else AfternoteButtonType.Un,
                    isLoading = state.isLoading,
                )
            }
        }
    }
}

/** 포토 피커 취소 결과(null)는 선택 변경이 아니므로 기존 프로필 이미지를 그대로 둔다. */
internal fun handleProfileImagePickerResult(
    uri: Uri?,
    onProfileImagePick: (Uri) -> Unit,
) {
    uri?.let(onProfileImagePick)
}
