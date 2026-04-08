package com.afternote.feature.onboarding.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.addFocusCleaner
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.R

/**
 * 온보딩 프로필 설정 화면.
 *
 * - 상단: [DetailTopBar] (Material 3 [androidx.compose.material3.CenterAlignedTopAppBar] 기반)
 * - 본문: 스크롤 + [ProfileImage] + 이름 입력 ([AfternoteTextField])
 * - 하단 CTA: [Scaffold]의 [bottomBar]로 고정, 시스템 내비게이션 영역은 [navigationBarsPadding] 처리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    nameState: TextFieldState,
    displayImageUri: String?,
    onEditProfileImageClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isCompleteEnabled by remember {
        derivedStateOf { nameState.text.isNotBlank() }
    }

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
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(AfternoteDesign.colors.white)
                        .navigationBarsPadding(),
            ) {
                AfternoteButton(
                    text = stringResource(R.string.profile_complete),
                    onClick = {
                        focusManager.clearFocus()
                        onCompleteClick()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .height(48.dp),
                    type =
                        if (isCompleteEnabled) {
                            AfternoteButtonType.Default
                        } else {
                            AfternoteButtonType.Un
                        },
                )
            }
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        OnboardingProfileContent(
            nameState = nameState,
            displayImageUri = displayImageUri,
            onEditProfileImageClick = onEditProfileImageClick,
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .fillMaxSize(),
        )
    }
}

@Composable
private fun OnboardingProfileContent(
    nameState: TextFieldState,
    displayImageUri: String?,
    onEditProfileImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .addFocusCleaner(focusManager)
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.profile_headline),
            modifier = Modifier.fillMaxWidth(),
            style =
                AfternoteDesign.typography.h1.copy(
                    fontWeight = FontWeight.Bold,
                ),
            color = AfternoteDesign.colors.gray9,
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(48.dp))

        ProfileImage(
            displayImageUri = displayImageUri,
            profileImageSize = 120.dp,
            isEditable = true,
            onEditClick = {
                focusManager.clearFocus()
                onEditProfileImageClick()
            },
        )

        Spacer(modifier = Modifier.height(56.dp))

        AfternoteTextField(
            state = nameState,
            placeholder = stringResource(R.string.profile_name_placeholder),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingProfileScreenPreview() {
    AfternoteTheme {
        OnboardingProfileScreen(
            nameState = rememberTextFieldState(),
            displayImageUri = null,
            onEditProfileImageClick = {},
            onCompleteClick = {},
            onBackClick = {},
        )
    }
}
