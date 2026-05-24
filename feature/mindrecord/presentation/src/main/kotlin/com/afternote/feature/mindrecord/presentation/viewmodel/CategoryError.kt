package com.afternote.feature.mindrecord.presentation.viewmodel

/**
 * DeepThoughtCategoryViewModel 가 화면에 노출하는 에러 상태.
 *
 * VM 은 "어떤 도메인 에러가 발생했는가" 만 표현하고, 사용자에게 보여줄 문자열로의 매핑은
 * [com.afternote.feature.mindrecord.presentation.screen.sender.DeepThoughtScreen] 같은
 * Composable 측에서 stringResource 로 수행한다.
 */
sealed interface CategoryError {
    /** 입력한 카테고리 이름이 trim 후 비어 있음 (도메인 검증 실패). */
    data object EmptyName : CategoryError

    /** 도메인 검증은 실패했지만 EmptyName 외 사유 (예: 길이/문자 제약). */
    data object NameInvalid : CategoryError

    /** 카테고리 목록 조회 실패. */
    data class ListFailed(
        val message: String?,
    ) : CategoryError

    /** 카테고리 추가 실패. */
    data class AddFailed(
        val message: String?,
    ) : CategoryError

    /** 카테고리 이름 변경 실패. */
    data class UpdateFailed(
        val message: String?,
    ) : CategoryError

    /** 카테고리 삭제 실패. */
    data class DeleteFailed(
        val message: String?,
    ) : CategoryError
}
