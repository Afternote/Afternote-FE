package com.afternote.feature.afternote.presentation.receiver.deliveryverification.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.ReceiverVerifyStep.IDENTITY

/**
 * 수신자 인증 흐름(designs 2·3·4·5·6·7·8·9)의 단계 상수.
 *
 * 본인 확인(1) → 마스터 키(2) → 서류 업로드(3) → 완료(4). 진행 인디케이터의 `currentStep` 인자에
 * 전달된다. `core/ui` 의 공용 [com.afternote.core.ui.scaffold.FlowStepScaffold] 사용 시
 * [RECEIVER_VERIFY_TOTAL_STEPS] 를 `totalSteps` 로 함께 넘긴다.
 *
 * 주의: 단계는 4개지만 화면은 5개다 — 본인 확인(1)은 Intro·Email 두 화면이 공유한다(둘 다 [IDENTITY]
 * 전달). 따라서 소개→이메일 전환 시 바가 25%에 멈춰 있고, 마지막 [COMPLETE](4)에서 바가 100%로 꽉 찬다
 * (완료를 바 숨김으로 두지 않고 진행을 끝까지 채워 보여준다).
 */
object ReceiverVerifyStep {
    const val IDENTITY: Int = 1
    const val MASTER_KEY: Int = 2
    const val DOCUMENTS: Int = 3
    const val COMPLETE: Int = 4
}

/** 수신자 인증 진행 단계 총수 (본인 확인·마스터 키·서류·완료). */
const val RECEIVER_VERIFY_TOTAL_STEPS: Int = 4

/**
 * 수신자 인증 흐름 공통: progress 아래·헤드라인 위 spacing.
 *
 * 5 화면(Intro·Email·MasterKey·DocumentUpload·Complete) 이 모두 동일 값 사용 — 디자인 시안상 흐름 단위 공통.
 * 회원가입 흐름은 화면별로 값이 달라(35/43/32) 별도 상수 없음.
 */
val RECEIVER_VERIFY_HEADER_SPACING: Dp = 35.dp
