package com.afternote.feature.afternote.presentation.shared.detail

/**
 * [MemorialVideoThumbnail] 오버레이 라벨의 테스트 태그.
 *
 * 라벨은 접근성 병합 노드가 장황해지지 않도록 `clearAndSetSemantics {}` 로 시맨틱을 비운다.
 * 그래서 텍스트로는 조회되지 않고 `onNodeWithTag(..., useUnmergedTree = true)` 만 통한다 — 태그는
 * 지울 수 없는 검증 수단이다.
 *
 * 선언을 컴포저블 파일에서 떼어 둔 이유는 `ProductionVisibilityKonsistTest`(#1678) 다. 같은 파일에서만
 * 쓰이면서 테스트가 참조하는 최상위 선언은 위반이고, 가드가 권하는 `private` 축소는 위 이유로 쓸 수
 * 없다. 파일을 나누면 [MemorialVideoThumbnail] 쪽이 「다른 프로덕션 파일」이 되어 조건이 깨진다.
 */
internal const val MEMORIAL_VIDEO_OVERLAY_LABEL_TEST_TAG = "memorialVideoOverlayLabel"
