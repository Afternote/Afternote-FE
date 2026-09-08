package com.afternote.feature.receiver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.receiver.presentation.deliveryverification.DeliveryVerificationCompleteScreen
import com.afternote.feature.receiver.presentation.deliveryverification.DeliveryVerificationFlowViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationIntroScreen
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyScreen
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute

/**
 * 열람 신청 흐름(본인 확인 → 마스터 키 → 서류 업로드 → 완료)의 **흐름 전용 로컬 스택**.
 *
 * Nav2 에서는 `navigation<DeliveryVerificationFlowRoute>` 중첩 그래프가 5개 화면의 공용
 * `ViewModelStore` 를 갖고 있었다. Nav3 엔 중첩 그래프가 없으므로, 흐름 키를 바깥 스택의 한
 * entry 로 두고 **그 entry 안에서 다시 스택을 연다** — 흐름 ViewModel 은 이 entry 범위라
 * 단계 사이에는 공유되고 흐름을 벗어나면(= entry 가 pop 되면) 정리된다. 이관 전과 같은 수명이다.
 *
 * 단계 사이 «pop 하고 다음으로» 는 Nav2 의 `popUpTo(inclusive = true)` 자리로, 로컬 스택에선
 * 결과 스택을 직접 만든다 — 뒤로가기로 이미 지나온 인증·마스터 키 화면에 돌아가지 못하게 하는
 * 기존 동작을 그대로 지킨다.
 *
 * @param key 흐름 진입 키. `senderId` 를 나르며 흐름 ViewModel 에 assisted 로 주입된다.
 * @param onExitFlow 흐름 스택 바닥에서의 back — 바깥 스택이 이 흐름 entry 를 내린다.
 * @param onExitToReceivedRecords 완료 화면의 "받은 기록함으로" — 바깥 스택을 받은 기록함까지 되감는다.
 */
@Composable
internal fun DeliveryVerificationFlowHost(
    key: ReceiverRoute.DeliveryVerificationFlowRoute,
    onExitFlow: () -> Unit,
    onExitToReceivedRecords: () -> Unit,
) {
    val flowViewModel =
        hiltViewModel<DeliveryVerificationFlowViewModel, DeliveryVerificationFlowViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        )
    val stepStack = rememberNavBackStack(ReceiverRoute.IdentityVerificationIntroRoute)
    val boundary = remember(onExitFlow) { FeatureStackBoundary(onExitFlow) }
    val actions =
        remember(stepStack, boundary, onExitToReceivedRecords) {
            DeliveryVerificationFlowLocalNavActions(stepStack, boundary, onExitToReceivedRecords)
        }

    FeatureNavDisplay(
        backStack = stepStack,
        boundary = boundary,
        entryProvider =
            entryProvider {
                entry<ReceiverRoute.IdentityVerificationIntroRoute> {
                    val isVerified by flowViewModel.isIdentityVerified.collectAsStateWithLifecycle()
                    // 이미 본인 확인을 마친 사용자는 안내 화면을 건너뛴다.
                    if (isVerified) {
                        LaunchedEffect(Unit) {
                            actions.proceedToMasterKey()
                        }
                    } else {
                        IdentityVerificationIntroScreen(
                            onBackClick = actions::popBack,
                            onStartClick = actions::navigateToIdentityVerificationEmail,
                        )
                    }
                }

                entry<ReceiverRoute.IdentityVerificationEmailRoute> {
                    IdentityVerificationEmailScreen(
                        senderId = flowViewModel.senderId,
                        onBackClick = actions::popBack,
                        onVerified = actions::proceedToMasterKey,
                    )
                }

                entry<ReceiverRoute.MasterKeyRoute> {
                    MasterKeyScreen(
                        senderId = flowViewModel.senderId,
                        onBackClick = actions::popBack,
                        onVerified = actions::proceedToDocumentUpload,
                    )
                }

                entry<ReceiverRoute.DocumentUploadRoute> {
                    DocumentUploadScreen(
                        onBackClick = actions::popBack,
                        onSubmitted = actions::proceedToDeliveryVerificationComplete,
                    )
                }

                entry<ReceiverRoute.DeliveryVerificationCompleteRoute> {
                    DeliveryVerificationCompleteScreen(
                        onBackToRecords = actions::popToReceivedRecords,
                    )
                }
            },
    )
}

/** 열람 신청 흐름 안에서만 의미가 있는 이동. 바깥 스택을 건드리는 둘은 콜백으로 위임한다. */
internal interface DeliveryVerificationFlowNavActions {
    fun popBack()

    /** 본인 확인 안내(2)의 "인증 시작하기" → 이메일 인증 화면(3·4). */
    fun navigateToIdentityVerificationEmail()

    /** Intro 의 캐시 hit jump 또는 이메일 인증 성공 → 마스터 키(5). 본인 확인 화면들은 남기지 않는다. */
    fun proceedToMasterKey()

    /** 마스터 키 검증 성공 → 증빙 서류 업로드(6·7·8). 마스터 키 화면은 남기지 않는다. */
    fun proceedToDocumentUpload()

    /** 서류 제출 성공 → 완료(9). 업로드 화면은 남기지 않는다. */
    fun proceedToDeliveryVerificationComplete()

    /** 완료(9)의 "받은 기록함으로 돌아가기". */
    fun popToReceivedRecords()
}
