package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.modifierextention.dropShadow
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.DocumentSlotCard
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.DocumentSourceBottomSheet
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.ReceiverVerifyScaffold
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.ReceiverVerifyStep

/**
 * 증빙 서류 업로드 화면(designs 6·7·8) — 사망진단서 + 가족관계증명서 첨부 후 열람 신청 제출 (이슈 #215).
 *
 * 슬롯 영역 클릭 → BottomSheet(7) → 이미지/파일 선택 → ContentResolver 로 바이트 + 확장자 + 표시 이름 추출 →
 * presigned URL 업로드. 양쪽 슬롯의 fileUrl 이 채워지면 "다음" 활성, `submitDeliveryVerification` 호출 →
 * 완료 화면(9) 진입.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadScreen(
    onBackClick: () -> Unit,
    onSubmitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentUploadViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val contentResolver = remember(context) { context.contentResolver }

    // 현재 BottomSheet 가 열려있는 슬롯. non-null = 시트 표시 + 어느 슬롯이 트리거했는지 식별, null = 시트 닫힘.
    var sheetSlot by remember { mutableStateOf<DocumentSlot?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 디자인: 시트 상단 모서리 = 가족관계증명서 텍스트필드 하단 + 57dp.
    // 절대 좌표 anchor 가 작은 값이라 디바이스 사이즈 변동에 안정적. 측정해서 시트 높이 동적 계산.
    var familyFieldBottomPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetHeight: Dp? =
        if (familyFieldBottomPx > 0) {
            with(density) {
                val containerHeightDp = windowInfo.containerSize.height.toDp()
                val familyFieldBottomDp = familyFieldBottomPx.toDp()
                (containerHeightDp - familyFieldBottomDp - 57.dp).coerceAtLeast(120.dp)
            }
        } else {
            null
        }

    // BottomSheet 닫히면 sheetSlot 은 null 로 비워지지만 picker 콜백은 그 뒤에 비동기로 옴.
    // 어느 슬롯의 결과인지 콜백 시점에 알아야 해서 sheetSlot 과 별도로 들고 있는 슬롯 식별자.
    val pendingSlot = remember { mutableStateOf<DocumentSlot?>(null) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            val slot = pendingSlot.value ?: return@rememberLauncherForActivityResult
            pendingSlot.value = null
            if (uri != null) handlePickedUri(viewModel, contentResolver, slot, uri)
        }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            val slot = pendingSlot.value ?: return@rememberLauncherForActivityResult
            pendingSlot.value = null
            if (uri != null) handlePickedUri(viewModel, contentResolver, slot, uri)
        }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            DocumentUploadEvent.Submitted -> onSubmitted()
        }
    }

    val errorMessage = uiState.errorMessage ?: uiState.errorMessageRes?.let { stringResource(it) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.consumeError()
        }
    }

    DocumentUploadScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSlotClick = { slot -> sheetSlot = slot },
        onFamilyFieldBottomChanged = { familyFieldBottomPx = it },
        onSubmitClick = viewModel::submit,
        modifier = modifier,
    )

    // 디자인 7 — 슬롯 클릭 시 떠오르는 미디어 소스 선택 시트. "이미지 추가" / "파일 추가" 둘 중 하나 선택.
    sheetSlot?.let { slot ->
        ModalBottomSheet(
            onDismissRequest = { sheetSlot = null },
            sheetState = sheetState,
            containerColor = AfternoteDesign.colors.gray1,
            dragHandle = null,
        ) {
            DocumentSourceBottomSheet(
                onPickImage = {
                    sheetSlot = null
                    pendingSlot.value = slot
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onPickFile = {
                    sheetSlot = null
                    pendingSlot.value = slot
                    filePickerLauncher.launch("*/*")
                },
                sheetHeight = sheetHeight,
            )
        }
    }
}

private fun handlePickedUri(
    viewModel: DocumentUploadViewModel,
    contentResolver: ContentResolver,
    slot: DocumentSlot,
    uri: Uri,
) {
    val result = contentResolver.readDocumentUri(uri) ?: return
    viewModel.uploadDocument(
        slot = slot,
        bytes = result.bytes,
        extension = result.extension,
        displayName = result.displayName,
    )
}

@Composable
private fun DocumentUploadScreenContent(
    uiState: DocumentUploadUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSlotClick: (DocumentSlot) -> Unit,
    onFamilyFieldBottomChanged: (Int) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiverVerifyScaffold(
        actionButtonText = stringResource(R.string.receiver_verify_next_button),
        onBackClick = onBackClick,
        onActionClick = onSubmitClick,
        isActionEnabled = uiState.canSubmit,
        currentStep = ReceiverVerifyStep.DOCUMENTS,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.receiver_verify_document_upload_title),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.receiver_verify_document_upload_description),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray5,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DocumentSlotCard(
                title = stringResource(R.string.receiver_verify_death_cert_title),
                slot = uiState.deathCertificate,
                onPickClick = { onSlotClick(DocumentSlot.DeathCertificate) },
            )
            DocumentSlotCard(
                title = stringResource(R.string.receiver_verify_family_cert_title),
                slot = uiState.familyRelationCertificate,
                onPickClick = { onSlotClick(DocumentSlot.FamilyRelationCertificate) },
                modifier =
                    Modifier.onGloballyPositioned { coords ->
                        onFamilyFieldBottomChanged(coords.boundsInWindow().bottom.toInt())
                    },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentUploadEmptyPreview() {
    AfternoteTheme {
        DocumentUploadScreenContent(
            uiState = DocumentUploadUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSlotClick = {},
            onFamilyFieldBottomChanged = {},
            onSubmitClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentUploadFilledPreview() {
    AfternoteTheme {
        DocumentUploadScreenContent(
            uiState =
                DocumentUploadUiState(
                    deathCertificate =
                        DocumentSlotState(displayName = "사망진단서.jpeg", fileUrl = "https://x"),
                    familyRelationCertificate =
                        DocumentSlotState(displayName = "가족관계증명서.pdf", fileUrl = "https://y"),
                ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSlotClick = {},
            onFamilyFieldBottomChanged = {},
            onSubmitClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun DocumentUploadWithSheetOpenPreview() {
    AfternoteTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            DocumentUploadScreenContent(
                uiState = DocumentUploadUiState(),
                snackbarHostState = remember { SnackbarHostState() },
                onBackClick = {},
                onSlotClick = {},
                onFamilyFieldBottomChanged = {},
                onSubmitClick = {},
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .dropShadow(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            color = Color.Black.copy(alpha = 0.08f),
                            blur = 16.dp,
                            offsetY = (-4).dp,
                            offsetX = 0.dp,
                            spread = 0.dp,
                        ).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(AfternoteDesign.colors.gray1),
            ) {
                DocumentSourceBottomSheet(
                    onPickImage = {},
                    onPickFile = {},
                    sheetHeight = 360.dp,
                )
            }
        }
    }
}
