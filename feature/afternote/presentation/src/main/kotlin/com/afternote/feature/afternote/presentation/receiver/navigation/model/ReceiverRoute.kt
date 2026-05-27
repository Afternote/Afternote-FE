package com.afternote.feature.afternote.presentation.receiver.navigation.model

import kotlinx.serialization.Serializable

/**
 * 수신자 흐름 [com.afternote.core.ui.Route.Receiver] 그래프 내부 라우트.
 *
 * 진입점(수신자 전용 온보딩)은 디자인 미정으로 미연결 상태이며, 본 그래프는
 * 그래프 내부 라우팅(Home → AfternoteList → AfternoteDetail)만 정의한다.
 */
sealed interface ReceiverRoute {
    /** 수신자 대시보드 — 발신자 한 마디 + 마음의 기록·타임레터·애프터노트 섹션 카드. */
    @Serializable
    data object HomeRoute : ReceiverRoute

    /**
     * 받은 기록함 — 수신자가 등록·신청한 발신자 카드 리스트.
     *
     * Welcome 의 "전달 받은 기록 확인하기" 진입점. 본인 확인 캐시 상태와 무관하게 진입 가능
     * (본인 확인은 발신자별 열람 신청 시작 시점에 1회 진행).
     */
    @Serializable
    data object ReceivedRecordsRoute : ReceiverRoute

    /**
     * 발신자 등록(15·16) — 받은 기록함의 FAB 에서 진입. 사용자가 발신자에게 부여하는 별칭(라벨) 입력.
     *
     * 백엔드 *발신자 라벨 등록 API* 가 미확정이라 1단계는 클라 로컬 stub registry 에만 보관한다.
     */
    @Serializable
    data object SenderRegistrationRoute : ReceiverRoute

    /**
     * 발신자 상세(11·12) — 카드 클릭 진입. 열람 신청 상태에 따라 3가지 표시:
     * 신청 기록 없음(열람 불가) / PENDING(승인 대기) / APPROVED(열람 가능).
     *
     * `senderId` 는 [com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistry]
     * 의 로컬 식별자 (typed-safe routes 규약: SavedStateHandle 키 `senderId` 와 일치).
     */
    @Serializable
    data class SenderDetailRoute(
        val senderId: String,
    ) : ReceiverRoute

    /**
     * 열람 신청 흐름 — 본인 확인(2·3·4) + 마스터 키(5) + 서류 업로드(6·7·8) + 완료(9) 의 *nested graph 진입점*.
     *
     * 흐름 전체 동안 유지되는 `senderId` 는 본 라우트에만 보유 — 자식 라우트들은 parent backStackEntry 의
     * [com.afternote.feature.afternote.presentation.receiver.deliveryverification.DeliveryVerificationFlowViewModel]
     * 에서 receive. 자식 라우트에서 senderId 를 nav arg 로 중복 박지 않는다.
     *
     * 발신자 상세 "열람 신청하기" 진입점.
     */
    @Serializable
    data class DeliveryVerificationFlowRoute(
        val senderId: String,
    ) : ReceiverRoute

    /**
     * 본인 확인 안내 화면(design 2). 흐름 진입 시 본인 확인 캐시가 없으면 본 라우트로.
     * senderId 는 부모 [DeliveryVerificationFlowRoute] 에서 받는다.
     */
    @Serializable
    data object IdentityVerificationIntroRoute : ReceiverRoute

    /**
     * 본인 확인 이메일 인증 화면(designs 3·4). 인증 시작하기 → 진입.
     *
     * 인증 성공 시 [com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository]
     * 캐시가 켜져 이후 동일 사용자(폰)에서는 마스터 키로 직진. DataStore 영구 저장이라 앱 재시작 후에도 유지.
     */
    @Serializable
    data object IdentityVerificationEmailRoute : ReceiverRoute

    /**
     * 열람 신청 1단계: 마스터 키 입력(5).
     *
     * `verify(authCode)` 응답 성공 시 ReceiverIdentity 를 (부모 라우트의) senderId 카드에 결합하고 다음 단계로.
     */
    @Serializable
    data object MasterKeyRoute : ReceiverRoute

    /**
     * 열람 신청 2단계: 증빙 서류 업로드(6·7·8). 사망진단서 + 가족관계증명서 각 1건 첨부.
     */
    @Serializable
    data object DocumentUploadRoute : ReceiverRoute

    /**
     * 열람 신청 3단계: 완료(9). `submitDeliveryVerification` 결과 표시.
     * "받은 기록함으로 돌아가기" 버튼이 발신자 상세까지 pop 한다.
     */
    @Serializable
    data object DeliveryVerificationCompleteRoute : ReceiverRoute

    /** 수신한 애프터노트 페이지드 목록. */
    @Serializable
    data object AfternoteListRoute : ReceiverRoute

    /**
     * 수신 애프터노트 상세.
     *
     * 프로퍼티 이름은 [com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel] 의
     * `SavedStateHandle` 키(`afternoteId`)와 일치해야 한다(typed-safe routes 규약).
     */
    @Serializable
    data class AfternoteDetailRoute(
        val afternoteId: String,
    ) : ReceiverRoute
}
