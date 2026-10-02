package com.afternote.feature.setting.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.shared.component.ProfilePhotoWithAddBadge

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    (uiState as? ProfileEditUiState.Success)?.pendingEvent?.let { pendingEvent ->
        ObserveSignal(
            signal = pendingEvent,
            // 소비 Intent 에 처리한 신호를 실어, 늦게 도착한 소비가 새로 올라온 다른 신호를 지우지 않게 한다.
            consumed = ProfileEditIntent.ConsumeEvent(pendingEvent),
            onIntent = viewModel::onIntent,
        ) { event ->
            when (event) {
                ProfileEditEvent.UpdateSuccess -> onBackClick()
                ProfileEditEvent.UpdateFailure -> Unit
            }
        }
    }

    ProfileEditContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onWithdrawGuideClick = onWithdrawGuideClick,
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    uiState: ProfileEditUiState,
    onIntent: (ProfileEditIntent) -> Unit,
    onBackClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "프로필 설정",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
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
                    onUpdateClick = { name, phone -> onIntent(ProfileEditIntent.UpdateProfile(name, phone)) },
                    onWithdrawGuideClick = onWithdrawGuideClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is ProfileEditUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "프로필을 불러올 수 없습니다.")
                }
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
                ProfilePhotoWithAddBadge()
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
