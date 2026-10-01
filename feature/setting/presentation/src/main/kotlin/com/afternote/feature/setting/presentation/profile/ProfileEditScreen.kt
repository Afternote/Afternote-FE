package com.afternote.feature.setting.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.ProfileImagePicker
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.receiver.ReceiverPhoneValidation
import com.afternote.feature.setting.presentation.receiver.validateReceiverPhone
import com.afternote.feature.setting.presentation.shared.component.SettingLoadErrorContent
import kotlinx.coroutines.launch

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(ProfileEditIntent.RefreshOnReturn) }
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackScope = rememberCoroutineScope()
    val updateFailedMessage = stringResource(R.string.setting_profile_update_error)
    val onPickProfileImage =
        rememberProfileImagePicker(
            canAcceptPhoto = (uiState as? ProfileEditUiState.Success)?.isUpdating == false,
            onPhotoPicked = { viewModel.onIntent(ProfileEditIntent.SelectPhoto(it)) },
        )

    (uiState as? ProfileEditUiState.Success)?.pendingEvent?.let { pendingEvent ->
        ObserveSignal(
            signal = pendingEvent,
            // 소비 Intent 에 처리한 신호를 실어, 늦게 도착한 소비가 새로 올라온 다른 신호를 지우지 않게 한다.
            consumed = ProfileEditIntent.ConsumeEvent(pendingEvent),
            onIntent = viewModel::onIntent,
        ) { event ->
            when (event) {
                ProfileEditEvent.UpdateSuccess -> {
                    onBackClick()
                }

                ProfileEditEvent.UpdateFailure -> {
                    feedbackScope.launch { snackbarHostState.showSnackbar(updateFailedMessage) }
                }
            }
        }
    }

    ProfileEditContent(
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        onPickImageClick = onPickProfileImage,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onWithdrawGuideClick = onWithdrawGuideClick,
        modifier = modifier,
    )
}

@Composable
private fun ProfileEditContent(
    snackbarHostState: SnackbarHostState,
    uiState: ProfileEditUiState,
    onPickImageClick: () -> Unit,
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
                    onPickImageClick = onPickImageClick,
                    onUpdateClick = { name, phone -> onIntent(ProfileEditIntent.UpdateProfile(name, phone)) },
                    onWithdrawGuideClick = onWithdrawGuideClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is ProfileEditUiState.Error -> {
                SettingLoadErrorContent(
                    message = stringResource(R.string.setting_profile_load_error),
                    onRetry = { onIntent(ProfileEditIntent.RetryLoad) },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ProfileEditForm(
    state: ProfileEditUiState.Success,
    onPickImageClick: () -> Unit,
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
        // 편집하지 않은 필드만 새 서버 값으로 바꾸고 입력 중인 값과 커서를 보존한다.
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
    val isPhoneInvalid = phoneState.text.toString().validateReceiverPhone(isRequired = false) != ReceiverPhoneValidation.VALID

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
                ProfileImagePicker(
                    onPickClick = onPickImageClick,
                    displayImageUri = state.displayImageUri,
                )
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
                    keyboardType = KeyboardType.Phone,
                    isError = isPhoneInvalid,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isPhoneInvalid) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.setting_receiver_phone_invalid),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.error,
                    )
                }
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
                type = if (state.isUpdating || isPhoneInvalid) AfternoteButtonType.Un else AfternoteButtonType.Default,
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

/**
 * 갤러리 사진 선택기를 등록하고, 누르면 띄우는 함수를 돌려준다.
 *
 * 온보딩 프로필 화면과 같이 갤러리 전용이다(`PickVisualMedia.ImageOnly`). 취소 결과(`null`)는 선택
 * 변경이 아니라서 기존 사진을 그대로 둔다(온보딩 #1113·#1115 와 같은 처리).
 *
 * 돌아온 사진은 [canAcceptPhoto] 가 참일 때까지 [rememberSaveable] 에 보관했다가 넘긴다. 갤러리에
 * 다녀오는 사이 프로세스가 회수되면 결과가 새 ViewModel 의 프로필 조회보다 먼저 도착하고, 저장 중에
 * 돌아온 결과는 ViewModel 이 받지 않기 때문이다. 바로 넘기면 두 경우 모두 고른 사진이 사라진다.
 */
@Composable
private fun rememberProfileImagePicker(
    canAcceptPhoto: Boolean,
    onPhotoPicked: (String) -> Unit,
): () -> Unit {
    val queuedPhoto = rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnPhotoPicked by rememberUpdatedState(onPhotoPicked)

    LaunchedEffect(queuedPhoto.value, canAcceptPhoto) {
        val uri = queuedPhoto.value
        if (uri != null && canAcceptPhoto) {
            queuedPhoto.value = null
            currentOnPhotoPicked(uri)
        }
    }

    val launcher =
        rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
            uri?.let { queuedPhoto.value = it.toString() }
        }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    }
}
