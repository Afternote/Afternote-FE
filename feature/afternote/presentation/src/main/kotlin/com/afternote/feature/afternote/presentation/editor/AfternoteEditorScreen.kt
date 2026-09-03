package com.afternote.feature.afternote.presentation.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.afternote.core.ui.modifierextention.addFocusCleaner
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.rememberAfternoteEditorState

/**
 * 스낵바 한 줄에 붙이는 되돌림 액션.
 *
 * 실패를 알리기만 하고 끝내면 사용자가 되돌릴 방법이 없는 경우가 있다 — 썸네일이 그렇다(#1550).
 */
data class EditorSnackbarAction(
    val label: String,
    val onPerform: () -> Unit,
)

/**
 * 애프터노트 수정/작성 화면
 *
 * 피그마 디자인 기반:
 * - 헤더 (뒤로가기, 타이틀, 등록 버튼)
 * - 종류 선택 드롭다운
 * - 검색 가능한 서비스명 선택 바텀시트
 * - 계정 정보 입력 (아이디, 비밀번호)
 * - 계정 처리 방법 선택 (라디오 버튼)
 * - 처리 방법 리스트 (체크박스)
 * - 남기실 말씀 (동적 텍스트 입력 목록)
 *
 * 추억 플레이리스트 곡 목록은 [EditorFormState]의 추억 노트 전용 폼에 동기화된 스냅샷으로 표시한다.
 */

@Composable
fun AfternoteEditorScreen(
    form: EditorFormState,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    snackbarMessage: String?,
    onSnackbarMessageConsumed: () -> Unit,
    // 스낵바 쌍과 같은 이유로 기본값을 주지 않는다: 새 진입 경로가 검증 팝업 배선을 빠뜨리면
    // 조용히 팝업 없는 에디터가 되는 대신 컴파일 단계에서 걸리게 한다.
    validationMessage: String?,
    onValidationMessageConsumed: () -> Unit,
    content: @Composable (SnackbarHostState) -> Unit,
    modifier: Modifier = Modifier,
    state: AfternoteEditorState = rememberAfternoteEditorState(),
    snackbarAction: EditorSnackbarAction? = null,
    shouldDeferBaselineCapture: Boolean = false,
    snackbarMessageKey: Any? = snackbarMessage,
    isSubmitEnabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessageKey) {
        snackbarMessage?.let { message ->
            try {
                val result =
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = snackbarAction?.label,
                        withDismissAction = true,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    snackbarAction?.onPerform?.invoke()
                }
            } finally {
                // dismiss 뿐 아니라 화면 이탈로 취소돼도 소비해야, 복귀 시 이미 고친 오류의 stale 안내가 재표출되지 않는다.
                onSnackbarMessageConsumed()
            }
        }
    }

    // 작성 도중 이탈 가드: 진입 시점 스냅샷 대비 변경이 있으면 뒤로가기 시 이탈 확인 팝업을 띄운다.
    // '내용 존재'가 아니라 '변경' 기준인 이유는 수정 모드(prefill)에서 무변경 이탈에도 매번 경고하게
    // 되어서다. 스냅샷은 서버 프리필과 신규 작성 기본값 적용이 모두 끝난 후 1회 캡처하고,
    // 하위 화면 왕복·프로세스 복원에도 유지되도록 saveable 로 둔다.
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    var baselineContentSignature by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(shouldDeferBaselineCapture) {
        if (!shouldDeferBaselineCapture && baselineContentSignature == null) {
            baselineContentSignature = editorContentSignature(form, state)
        }
    }
    val hasUnsavedChanges by remember(form, baselineContentSignature) {
        derivedStateOf {
            baselineContentSignature != null &&
                editorContentSignature(form, state) != baselineContentSignature
        }
    }
    val onBackAttempt: () -> Unit = {
        focusManager.clearFocus()
        if (hasUnsavedChanges) {
            showExitConfirm = true
        } else {
            onBackClick()
        }
    }

    // 변경이 없을 때는 시스템 기본 뒤로가기를 유지한다 (predictive back 애니메이션 보존).
    BackHandler(enabled = hasUnsavedChanges) { onBackAttempt() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_editor_screen_title),
                onBackClick = onBackAttempt,
                actions = {
                    // 저장이 나가 있는 동안·prefill 을 못 읽은 동안은 «등록» 을 흐리고 눌리지 않게 한다 (#705).
                    // 유휴 상태와 같은 모습이면 사용자는 응답 없는 화면으로 읽고 연타로 중복 저장을 시도한다.
                    Text(
                        text = stringResource(R.string.afternote_editor_submit),
                        style = AfternoteDesign.typography.bodySmallB,
                        color =
                            if (isSubmitEnabled) {
                                AfternoteDesign.colors.gray9
                            } else {
                                AfternoteDesign.colors.gray5
                            },
                        modifier =
                            Modifier
                                .clickable(
                                    enabled = isSubmitEnabled,
                                    role = Role.Button,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onRegisterClick()
                                    },
                                ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .addFocusCleaner(focusManager),
        ) {
            content(snackbarHostState)

            if (showExitConfirm) {
                Popup(
                    type = PopupType.Variant2,
                    message = stringResource(R.string.afternote_editor_exit_confirm_message),
                    onConfirm = {
                        showExitConfirm = false
                        onBackClick()
                    },
                    onDismiss = { showExitConfirm = false },
                    confirmText = stringResource(R.string.afternote_editor_exit_confirm_confirm),
                    dismissText = stringResource(R.string.afternote_editor_exit_confirm_cancel),
                )
            }

            if (validationMessage != null) {
                Popup(
                    type = PopupType.Default,
                    message = validationMessage,
                    onConfirm = onValidationMessageConsumed,
                    onDismiss = onValidationMessageConsumed,
                )
            }
        }
    }
}

/** 카테고리 전용 입력이 비어 있을 때 싣는 고정 토큰. 지문 구분자와 겹치지 않는 제어문자. */
private const val NO_ENTERED_CONTENT = "\u0003"

/**
 * 사용자가 에디터에서 편집할 수 있는 값 전부를 한 줄 문자열로 직렬화한 상태 지문.
 * 진입 직후 값을 기준으로 저장해 두고, 뒤로가기 시점 값과 문자열 비교가 다르면 "작성 내용이
 * 바뀌었다"로 판단해 이탈 확인 팝업을 띄운다.
 *
 * 공용 폼은 필드를 골라 담지 않고 통째로 직렬화하되, 판정에서 뺄 것만 [EditorFormState.copy]로
 * 기본값 치환한다 — 폼에 필드가 새로 생기면 자동으로 판정에 포함되므로(빠짐 불가능),
 * 유지보수 대상은 아래 제외 목록뿐이다. 제외를 빠뜨리면 데이터 소실이 아니라
 * "팝업이 한 번 더 뜨는" 눈에 보이는 오탐으로 드러난다.
 *
 * 카테고리 전용 입력은 판별자 없이 "넣은 값" 으로만 싣는다 — 구경은 되돌리는 비용이 탭 한 번이라
 * 잃을 것이 없고, 값을 넣은 카테고리를 떠나면 그 값은 전환 시점에 이미 폐기되므로 지문이 달라진다.
 */
internal fun editorContentSignature(
    form: EditorFormState,
    state: AfternoteEditorState,
): String {
    val comparableForm =
        form.copy(
            // 카테고리 전용 입력은 아래에서 따로 낸다 — 전용 필드가 0개인 ESTATE 를 중립 원소로 쓴다.
            typeForm = AfternoteTypeForm.Estate,
        )
    return listOf(
        comparableForm.toString(),
        form.typeForm.enteredContentOrNull() ?: NO_ENTERED_CONTENT,
        state.idState.text,
        state.passwordState.text,
        state.editorMessages.map {
            "${it.titleState.text}\u0001${it.contentState.text}\u0001${it.isRegistered}"
        },
    ).joinToString("\u0002")
}
