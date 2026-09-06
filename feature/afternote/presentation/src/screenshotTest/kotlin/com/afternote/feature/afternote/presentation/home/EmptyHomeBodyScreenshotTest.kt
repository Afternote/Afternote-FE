package com.afternote.feature.afternote.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.android.tools.screenshot.PreviewTest

/**
 * 애프터노트 0건 + 카테고리 필터 없음 — 첫 진입 사용자가 보는 본문 (#1175).
 *
 * 종전 baseline 에는 이 상태가 없었다. 헤더가 `InfiniteListBody` 안에만 있어 이 경로는 빈 그림·문구만
 * 그렸고, 화면 제목(`afternote_home_title`)과 설명이 한 번도 렌더되지 않았다. 시안
 * [`애프터노트_목록X` 4327:66762](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4327-66762)
 * 는 0건에서도 제목·설명·NEXT STEP **과 카테고리 필터 행**을 그대로 두므로 그 상태를 골든으로 고정한다.
 *
 * 골든이 잡아야 할 것은 문구 두 종이 섞이지 않는다는 점이다 — 이 상태의 본문 문구는
 * `afternote_empty_list_body`(전체 0건)여야 하고, 필터 행이 생겼다고 해서
 * `afternote_home_filtered_empty`(카테고리 필터 결과 0건)로 바뀌면 안 된다 (#567).
 *
 * NEXT STEP 은 `null` 이다 — 문구를 만드는 원천이 아직 없다(Afternote-BE#270). 카드가 붙은 모습은
 * [homeHeaderSectionScreenshot] 이 이미 덮고 있다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun emptyHomeBodyScreenshot() {
    AfternoteTheme {
        EmptyHomeBody(
            headerDescription = stringResource(R.string.afternote_home_header_description),
            nextStep = null,
            emptyListDescription = stringResource(R.string.afternote_empty_list_body),
            onTypeSelected = {},
        )
    }
}
