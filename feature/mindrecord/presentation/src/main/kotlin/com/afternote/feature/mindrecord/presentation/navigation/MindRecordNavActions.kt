package com.afternote.feature.mindrecord.presentation.navigation

/**
 * NavHost 루트에서 마인드레코드 서브그래프로 넘기는 네비게이션 명령 모음.
 *
 * #924 Nav3 파일럿으로 허브 내부 이동(작성·임시저장 목록의 push/pop 7개)이 로컬 백스택으로
 * 흡수되어, 루트 NavController 가 필요한 명령만 남았다.
 */
interface MindRecordNavActions {
    fun onMemorySpaceBack()
}
