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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.ProfileImagePicker
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar

@Composable
internal fun ProfileEditScreen(
    onBackClick: () -> Unit,
    onWithdrawGuideClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBackClick by rememberUpdatedState(onBackClick)
    // when 분기 밖에서 만든다. 분기 안에 두면 상태가 바뀔 때 런처 등록과 보관 중인 결과가 함께 사라진다.
    val onPickProfileImage =
        rememberProfileImagePicker(
            canAcceptPhoto = (uiState as? ProfileEditUiState.Success)?.isUpdating == false,
            onPhotoPicked = viewModel::selectProfileImage,
        )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEditEvent.UpdateSuccess -> currentOnBackClick()
                ProfileEditEvent.UpdateFailure -> Unit
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
                    onPickImageClick = onPickProfileImage,
                    onUpdateClick = viewModel::updateProfile,
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
    onPickImageClick: () -> Unit,
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
