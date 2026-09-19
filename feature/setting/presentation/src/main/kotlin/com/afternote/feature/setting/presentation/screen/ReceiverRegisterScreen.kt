package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PhoneNumberInputTransformation
import com.afternote.core.ui.PhoneNumberVisualTransformation
import com.afternote.core.ui.UiText
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.KakaoContainerColor
import com.afternote.feature.setting.presentation.component.KakaoContentColor
import com.afternote.feature.setting.presentation.component.ProfilePhotoWithAddBadge
import com.afternote.feature.setting.presentation.component.ReceiverInviteSheet
import com.afternote.feature.setting.presentation.social.shareReceiverInvitationViaKakao
import com.afternote.feature.setting.presentation.viewmodel.ReceiverInviteIntent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverInviteViewModel
import com.afternote.feature.setting.presentation.viewmodel.ReceiverPhoneValidation
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterEvent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterViewModel
import com.afternote.feature.setting.presentation.viewmodel.isValidReceiverEmail
import com.afternote.feature.setting.presentation.viewmodel.validateReceiverPhone
import kotlinx.coroutines.launch
import com.afternote.core.ui.R as CoreR

private const val CUSTOM_RELATION_OPTION = "직접 추가하기"
private val relationOptions = listOf("어머니", "아버지", "아들", "딸", CUSTOM_RELATION_OPTION)

/**
 * 수신자 등록. 폼(이메일 «등록») 과 카카오톡 초대(폼 아래 CTA → 시트 → «초대를 보냈어요») 를 한 화면이 갖는다 (#944).
 *
 * «초대를 보냈어요»(4996:39909) 는 별도 destination 이 아니라 이 화면의 phase 다 — 설정 네비게이션 축은
 * Nav3 이관(#1695)이 소유해 Nav2 표면을 새로 만들지 않는다. 그 화면의 back·«수신자 목록으로 이동하기» 는
 * 둘 다 [onBackClick] 이다(등록 진입 전 화면으로 돌아간다).
 */
@Composable
fun ReceiverRegisterScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverRegisterViewModel = hiltViewModel(),
    inviteViewModel: ReceiverInviteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inviteState by inviteViewModel.uiState.collectAsStateWithLifecycle()
    val currentOnRegisterSuccess by rememberUpdatedState(onRegisterSuccess)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareFailedMessage = UiText.Resource(R.string.receiver_invite_share_failed)
    val shareFailedText = stringResource(R.string.receiver_invite_share_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ReceiverRegisterEvent.RegisterSuccess -> currentOnRegisterSuccess()
            }
        }
    }

    // 초대가 만들어졌다(또는 다시 보내기) — 카카오 SDK 가 Activity 컨텍스트를 요구해 공유는 여기서 띄운다.
    // suspend 를 effect 안에서 기다리지 않고 별도 scope 에 띄운다 — 소비 직후 상태 변화가 effect 를 재시작시킨다.
    ObserveSignal(
        signal = inviteState.shareRequest,
        consumed = ReceiverInviteIntent.ConsumeShareRequest,
        onIntent = inviteViewModel::onIntent,
    ) { request ->
        val isResend = inviteState.sentInvitation != null
        scope.launch {
            shareReceiverInvitationViaKakao(context, token = request.token, senderName = request.senderName)
                .onSuccess {
                    if (!isResend) inviteViewModel.onIntent(ReceiverInviteIntent.ShareLaunched)
                }.onFailure { cause ->
                    // 공유를 못 띄웠다(취소·카카오톡 없음·SDK 오류) — 원인은 ViewModel 이 진단에 남기고,
                    // 시트면 안내를 얹고, 보냈어요 화면이면 스낵바.
                    inviteViewModel.onIntent(ReceiverInviteIntent.ShareFailed(shareFailedMessage, cause))
                    if (isResend) snackbarHostState.showSnackbar(shareFailedText)
                }
        }
    }

    val sentInvitation = inviteState.sentInvitation
    if (sentInvitation != null) {
        ReceiverInviteSentContent(
            receiverName = sentInvitation.receiverName,
            snackbarHostState = snackbarHostState,
            onBackClick = onBackClick,
            onOpenReceiverList = onBackClick,
            onResend = { inviteViewModel.onIntent(ReceiverInviteIntent.Resend) },
            modifier = modifier,
        )
        return
    }

    inviteState.sheetReceiverName?.let { receiverName ->
        ReceiverInviteSheet(
            receiverName = receiverName,
            isSending = inviteState.isCreating,
            errorMessage = inviteState.errorMessage,
            onSend = { inviteViewModel.onIntent(ReceiverInviteIntent.SendInvite) },
            onDismiss = { inviteViewModel.onIntent(ReceiverInviteIntent.CloseSheet) },
        )
    }

    ReceiverRegisterContent(
        title = "수신자 등록",
        actionText = "등록",
        isPhoneRequired = true,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onRegister = viewModel::register,
        onInviteClick = { name -> inviteViewModel.onIntent(ReceiverInviteIntent.OpenSheet(name)) },
        modifier = modifier,
    )
}

@Composable
internal fun ReceiverRegisterContent(
    title: String,
    actionText: String,
    isPhoneRequired: Boolean,
    isLoading: Boolean,
    errorMessage: UiText?,
    onBackClick: () -> Unit,
    onRegister: (name: String, relation: String, phone: String, email: String, message: String) -> Unit,
    onInviteClick: ((name: String) -> Unit)?,
    modifier: Modifier = Modifier,
    initialName: String = "",
    initialRelation: String = "",
    initialPhone: String = "",
    initialEmail: String = "",
    initialMessage: String = "",
) {
    val isPresetRelation = initialRelation in relationOptions
    val nameState = rememberTextFieldState(initialText = initialName)
    val phoneState = rememberTextFieldState(initialText = initialPhone.filter(Char::isDigit))
    val emailState = rememberTextFieldState(initialText = initialEmail)
    val messageState = rememberTextFieldState(initialText = initialMessage)
    val customRelationState = rememberTextFieldState(initialText = initialRelation.takeUnless { isPresetRelation }.orEmpty())
    var selectedRelation by
        remember(initialRelation) {
            mutableStateOf(
                when {
                    initialRelation.isBlank() -> null
                    isPresetRelation -> initialRelation
                    else -> CUSTOM_RELATION_OPTION
                },
            )
        }
    var relationExpanded by remember { mutableStateOf(false) }

    val relation =
        when (selectedRelation) {
            CUSTOM_RELATION_OPTION -> customRelationState.text.toString().trim()
            else -> selectedRelation.orEmpty()
        }
    val phone = phoneState.text.toString()
    val phoneValidation = phone.validateReceiverPhone(isRequired = isPhoneRequired)
    val email = emailState.text.toString()
    val isEmailValid = email.isValidReceiverEmail()
    val isFormValid =
        nameState.text.isNotBlank() &&
            relation.isNotBlank() &&
            isEmailValid &&
            phoneValidation == ReceiverPhoneValidation.VALID

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = title,
                onBackClick = onBackClick,
                actions = {
                    TextButton(
                        onClick = {
                            onRegister(
                                nameState.text.toString(),
                                relation,
                                phone,
                                email,
                                messageState.text.toString(),
                            )
                        },
                        enabled = isFormValid && !isLoading,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = AfternoteDesign.colors.gray9,
                                disabledContentColor = AfternoteDesign.colors.gray2,
                            ),
                    ) {
                        Text(
                            text = actionText,
                            style = AfternoteDesign.typography.bodyLargeB,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ProfilePhotoWithAddBadge()
            }
            item {
                Spacer(modifier = Modifier.height(56.dp))
                Text("이름", modifier = Modifier.fillMaxWidth())
                AfternoteTextField(
                    state = nameState,
                    placeholder = "이름을 입력하세요",
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("연락처", modifier = Modifier.fillMaxWidth())
                AfternoteTextField(
                    state = phoneState,
                    placeholder = "연락처를 지정해주세요",
                    keyboardType = KeyboardType.Phone,
                    inputTransformation = PhoneNumberInputTransformation,
                    outputTransformation = PhoneNumberVisualTransformation,
                )
                if (phoneValidation != ReceiverPhoneValidation.VALID) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text =
                            stringResource(
                                if (phoneValidation == ReceiverPhoneValidation.REQUIRED) {
                                    R.string.receiver_phone_required
                                } else {
                                    R.string.receiver_phone_invalid
                                },
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.error,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("관계", modifier = Modifier.fillMaxWidth())
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { relationExpanded = true }
                                .padding(top = 13.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedRelation ?: "관계를 선택하세요",
                                style = AfternoteDesign.typography.bodyBase,
                                color =
                                    if (selectedRelation == null) {
                                        AfternoteDesign.colors.gray4
                                    } else {
                                        AfternoteDesign.colors.gray9
                                    },
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                painter = painterResource(CoreR.drawable.core_ui_arrowdown),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(18.dp)
                                        .rotate(if (relationExpanded) 180f else 0f),
                                tint = AfternoteDesign.colors.gray6,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            thickness = 0.8.dp,
                            color = AfternoteDesign.colors.gray3,
                        )
                    }
                    DropdownMenu(
                        expanded = relationExpanded,
                        onDismissRequest = { relationExpanded = false },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(AfternoteDesign.colors.white),
                    ) {
                        relationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        style = AfternoteDesign.typography.textField,
                                        color = AfternoteDesign.colors.gray9,
                                    )
                                },
                                onClick = {
                                    selectedRelation = option
                                    relationExpanded = false
                                },
                            )
                        }
                    }
                }
                if (selectedRelation == CUSTOM_RELATION_OPTION) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AfternoteTextField(
                        state = customRelationState,
                        placeholder = stringResource(R.string.receiver_custom_relation_placeholder),
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("이메일", modifier = Modifier.fillMaxWidth())
                AfternoteTextField(
                    state = emailState,
                    placeholder = "afternote@email.com",
                )
                if (!isEmailValid) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text =
                            stringResource(
                                if (email.isBlank()) {
                                    R.string.receiver_email_required
                                } else {
                                    R.string.receiver_email_invalid
                                },
                            ),
                        modifier = Modifier.fillMaxWidth(),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.error,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.receiver_last_greeting_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                AfternoteTextField(
                    state = messageState,
                    placeholder = stringResource(R.string.receiver_last_greeting_placeholder),
                )
            }
            if (onInviteClick != null) {
                item {
                    ReceiverInviteCta(
                        isNameFilled = nameState.text.isNotBlank(),
                        onClick = { onInviteClick(nameState.text.toString().trim()) },
                    )
                }
            }
            if (errorMessage != null) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage.asString(),
                        modifier = Modifier.fillMaxWidth(),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.error,
                    )
                }
            }
        }
    }
}

/**
 * 폼 아래의 카카오톡 초대 진입 (시안 4996:39735 하단, #944). 이메일 «등록» 흐름과 별개다 —
 * 폼의 나머지 칸을 로컬에 보관하는 일은 PM·BE 결정 대기(#1948)라 여기서 하지 않는다.
 *
 * 이름이 비어 있으면 누르지 못한다 — 시트 본문과 «초대를 보냈어요» 가 그 이름을 부른다.
 */
@Composable
private fun ReceiverInviteCta(
    isNameFilled: Boolean,
    onClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(40.dp))
    Text(
        text = stringResource(R.string.receiver_invite_form_notice),
        style = AfternoteDesign.typography.captionLargeR,
        color = AfternoteDesign.colors.gray6,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AfternoteDesign.colors.gray2)
                .padding(16.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))
    AfternoteButton(
        text = stringResource(R.string.receiver_invite_cta),
        onClick = onClick,
        type = if (isNameFilled) AfternoteButtonType.Default else AfternoteButtonType.Un,
        containerColor = if (isNameFilled) KakaoContainerColor else null,
        contentColor = if (isNameFilled) KakaoContentColor else null,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(24.dp))
}

