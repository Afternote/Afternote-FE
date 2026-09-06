package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SettingLoadErrorContent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditEvent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    val snackbarHostState = remember { SnackbarHostState() }
    val updateErrorMessage = stringResource(R.string.setting_profile_update_error)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEditEvent.UpdateSuccess -> currentOnBackClick()
                ProfileEditEvent.UpdateFailure -> snackbarHostState.showSnackbar(updateErrorMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "프로필 설정",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val state = uiState) {
            is ProfileEditUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileEditUiState.Success -> {
                ProfileEditForm(
                    state = state,
                    onUpdateClick = viewModel::updateProfile,
                    onWithdrawGuideClick = onWithdrawGuideClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is ProfileEditUiState.Error -> {
                SettingLoadErrorContent(
                    message = stringResource(R.string.setting_profile_load_error),
                    onRetry = viewModel::retryLoadProfile,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ProfileEditForm(
    state: ProfileEditUiState.Success,
    onUpdateClick: (name: String, phone: String) -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameState = rememberTextFieldState(initialText = state.name)
    val phoneState = rememberTextFieldState(initialText = state.phone)
    val emailState = rememberTextFieldState(initialText = state.email)
    var previousName by rememberSaveable { mutableStateOf(state.name) }
    var previousPhone by rememberSaveable { mutableStateOf(state.phone) }

    LaunchedEffect(state.name, state.phone, state.email) {
        // 서버 값과 같던 필드만 갱신해 작성 중인 입력과 커서를 보존한다.
        if (nameState.text.toString() == previousName && previousName != state.name) {
            nameState.setTextAndPlaceCursorAtEnd(state.name)
        }
        if (phoneState.text.toString() == previousPhone && previousPhone != state.phone) {
            phoneState.setTextAndPlaceCursorAtEnd(state.phone)
        }
        if (emailState.text.toString() != state.email) emailState.setTextAndPlaceCursorAtEnd(state.email)
        previousName = state.name
        previousPhone = state.phone
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(134.dp)) {
                    Image(
                        painter = painterResource(R.drawable.ic_default_profile),
                        contentDescription = "기본",
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = "추가",
                            modifier = Modifier.requiredSize(72.dp),
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.padding(top = 58.dp))
        }
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "이름",
                    style = AfternoteDesign.typography.bodySmallR,
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                AfternoteTextField(
                    state = nameState,
                    placeholder = "이름을 지정해주세요",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = "연락처", style = AfternoteDesign.typography.bodySmallR)
                Spacer(modifier = Modifier.padding(top = 8.dp))
                AfternoteTextField(
                    state = phoneState,
                    placeholder = "연락처를 지정해주세요",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = "이메일", style = AfternoteDesign.typography.bodySmallR)
                Spacer(modifier = Modifier.padding(top = 8.dp))
                AfternoteTextField(
                    state = emailState,
                    placeholder = "이메일을 지정해주세요",
                    modifier = Modifier.fillMaxWidth(),
                    inputTransformation = InputTransformation { revertAllChanges() },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(56.dp))
            AfternoteButton(
                text = "수정하기",
                onClick = { onUpdateClick(nameState.text.toString(), phoneState.text.toString()) },
                type = if (state.isUpdating) AfternoteButtonType.Un else AfternoteButtonType.Default,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
            )
        }
        item {
            Spacer(modifier = Modifier.height(54.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onWithdrawGuideClick),
                ) {
                    Text(
                        "회원탈퇴하기",
                        style = AfternoteDesign.typography.textField,
                        color = AfternoteDesign.colors.gray4,
                    )
                    HorizontalDivider(
                        modifier = Modifier.width(80.dp),
                        thickness = 1.dp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPrev() {
    Scaffold(
        topBar = { DetailTopBar(title = "프로필 설정") },
    ) { innerPadding ->
        ProfileEditForm(
            state =
                ProfileEditUiState.Success(
                    name = "박서연",
                    phone = "01012345678",
                    email = "afternote@email.com",
                ),
            onUpdateClick = { _, _ -> },
            onWithdrawGuideClick = {},
            modifier = Modifier.padding(innerPadding),
        )
    }
}
