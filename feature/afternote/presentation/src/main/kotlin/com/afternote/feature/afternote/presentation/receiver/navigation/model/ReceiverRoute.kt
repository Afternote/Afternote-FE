package com.afternote.feature.afternote.presentation.receiver.navigation.model

import kotlinx.serialization.Serializable

/**
 * 수신자 흐름 [com.afternote.core.ui.Route.Receiver] 그래프 내부 라우트.
 *
 * 온보딩 Welcome 의 "전달 받은 기록 확인하기" 콜백이 [com.afternote.core.ui.Route.Receiver]
 * (= startDestination [ReceivedRecordsRoute]) 로 진입한다.
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
     * 발신자 상세(11·12) — 카드 클릭 진입. 열람 신청 상태에 따라 3가지 표시:
     * 신청 기록 없음(열람 불가) / PENDING(승인 대기) / APPROVED(열람 가능).
     *
     * `senderId` 는 서버 `receiverId`로 만든 받은 기록함 항목 식별자다
     * (typed-safe routes 규약: SavedStateHandle 키 `senderId` 와 일치).
     */
    @Serializable
    data class SenderDetailRoute(
        val senderId: String,
    ) : ReceiverRoute

    /**
     * 열람 신청 흐름 — 본인 확인(2·3·4) + 마스터 키(5) + 서류 업로드(6·7·8) + 완료(9) 의 *nested graph 진입점*.
     *
     * 시작 카드의 `senderId` 는 본 graph route 에만 두어 어느 상세 화면에서 시작한 흐름인지 식별한다.
     * 실제 API 컨텍스트는 마스터 키 검증 성공 시 저장되는 접근 코드가 담당하며, 자식 라우트에
     * `senderId` 를 중복 전달하지 않는다.
     *
     * 발신자 상세 "열람 신청하기" 진입점.
     */
    @Serializable
    data class DeliveryVerificationFlowRoute(
        val senderId: String,
    ) : ReceiverRoute

    /**
     * 본인 확인 안내 화면(design 2). 흐름 진입 시 본인 확인 캐시가 없으면 본 라우트로.
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
     * `verify(authCode)` 성공 시 접근 코드를 저장하고 다음 단계로 이동한다. 받은 기록함은 저장된 코드로
     * 서버 `record-boxes` 목록을 다시 불러온다.
     */
    @Serializable
    data object MasterKeyRoute : ReceiverRoute

    /**
     * 열람 신청 2단계: 증빙 서류 업로드(6·7·8). 사망진단서 / 가족관계증명서 중 하나 이상 첨부 (이슈 #380).
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

    /**
     * 수신 추억 플레이리스트 — 추억 상세
     * ([com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen]) 의
     * "추억 플레이리스트" 카드 클릭 진입.
     *
     * 프로퍼티 이름은
     * [com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel] 의
     * `SavedStateHandle` 키(`afternoteId`)와 일치해야 한다(typed-safe routes 규약).
     */
    @Serializable
    data class MemorialPlaylistRoute(
        val afternoteId: String,
    ) : ReceiverRoute
}
