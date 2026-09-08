package com.afternote.feature.home.presentation.receiver

/**
 * 수신자 홈에서 외부 라우팅으로 빠지는 액션 묶음.
 *
 * 데이터 로드/다운로드는 ViewModel이 관할하고, 외부 화면 이동만 호출 측이 채운다.
 */
data class ReceiverHomeActions(
    val onNavigateToMindRecord: () -> Unit,
    val onNavigateToTimeLetter: () -> Unit,
    val onNavigateToAfternote: () -> Unit,
)
