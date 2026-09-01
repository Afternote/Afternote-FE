package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.PhoneNumberInputTransformation
import com.afternote.core.ui.PhoneNumberVisualTransformation
import com.afternote.core.ui.UiText
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.ReceiverPhoneValidation
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterEvent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverRegisterViewModel
import com.afternote.feature.setting.presentation.viewmodel.isValidReceiverEmail
import com.afternote.feature.setting.presentation.viewmodel.validateReceiverPhone
import com.afternote.core.ui.R as CoreR

private const val CUSTOM_RELATION_OPTION = "직접 추가하기"
private val relationOptions = listOf("어머니", "아버지", "아들", "딸", CUSTOM_RELATION_OPTION)

@Composable
fun ReceiverRegisterScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiverRegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnRegisterSuccess by rememberUpdatedState(onRegisterSuccess)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ReceiverRegisterEvent.RegisterSuccess -> currentOnRegisterSuccess()
            }
        }
    }

    ReceiverRegisterContent(
        title = "수신자 등록",
        actionText = "등록",
        isPhoneRequired = true,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onBackClick = onBackClick,
        onRegister = viewModel::register,
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

@Preview(showBackground = true)
@Composable
private fun ReceiverRegisterContentPreview() {
    AfternoteTheme {
        ReceiverRegisterContent(
            title = "수신자 등록",
            actionText = "등록",
            isPhoneRequired = true,
            isLoading = false,
            errorMessage = null,
            onBackClick = {},
            onRegister = { _, _, _, _, _ -> },
        )
    }
}
