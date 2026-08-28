package com.afternote.feature.receiver.presentation.home

/**
 * 수신자 홈에서 외부 라우팅으로 빠지는 액션 묶음.
 *
 * 데이터 로드/다운로드는 ViewModel이 관할하고, 외부 화면 이동만 호출 측이 채운다.
 */
data class ReceiverHomeActions(
    val onSettingClick: () -> Unit,
    val onNavigateToMindRecord: () -> Unit,
    val onNavigateToTimeLetter: () -> Unit,
    val onNavigateToAfternote: () -> Unit,
) {
    companion object {
        /**
         * 프리뷰·테스트 전용 no-op 묶음. 프로덕션 호출부는 no-op 디폴트에 기대지 말고
         * 전 액션을 명시적으로 채워야 한다 (#1388 — 배선 누락이 컴파일을 통과하는 통로 차단).
         */
        val Noop =
            ReceiverHomeActions(
                onSettingClick = {},
                onNavigateToMindRecord = {},
                onNavigateToTimeLetter = {},
                onNavigateToAfternote = {},
            )
    }
}
