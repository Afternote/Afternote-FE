package com.afternote.feature.afternote.presentation.shared.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 카테고리 필터 없이 목록이 0건일 때의 본문.
 *
 * @param description 안내 문구. 기본값을 두지 않는다 — 이 컴포저블은 작성자와 수신자가 공유하는데
 *   두 관점의 문구가 다르다. 발신자 문구(`afternote_empty_list_body`)는 «아래 연필 버튼을 눌러»
 *   로 끝나지만 수신자 화면에는 그 FAB 이 없어(`ReceiverAfternoteHomeEntry` 가 `onFabClick` 을 넘기지
 *   않는다) 누를 것이 없는 동작을 시키는 문구가 됐다 — 발신자 문구가 수신자에게 새던 #620 과 같은 부류다.
 *   디폴트를 걷어 호출부가 매번 자기 관점을 적게 한다 — `HomeHeaderSection` 의 `description`·
 *   `AfternoteHomeScreen` 의 `showsHeaderOnEmptyList` 와 같은 규칙이다.
 */
@Composable
fun EmptyListBody(
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(start = 20.dp, top = 24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.afternote_ic_empty_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .size(width = 228.dp, height = 56.dp),
            tint = AfternoteDesign.colors.gray8,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = description,
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
        Spacer(Modifier.weight(1f))
    }
}
