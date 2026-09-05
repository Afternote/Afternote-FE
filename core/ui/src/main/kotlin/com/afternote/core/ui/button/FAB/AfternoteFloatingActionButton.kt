package com.afternote.core.ui.button.FAB

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 화면 우측 하단에 떠 있는 대장 버튼(FAB).
 *
 * [size] 기본값은 M3 [FloatingActionButton] 기본(56dp)이라 기존 호출부는 무변경이다.
 * 시안(plus_button 48×48, 글리프 16.67dp)에 맞추려면 호출부에서 [size] = 48.dp, [iconSize] = 17.dp 로 opt-in 한다.
 * (벡터 viewport 14 / 글리프 13.6 을 Icon 이 ContentScale.Fit 로 확대 → 17 × 13.6/14 ≈ 16.5dp.)
 */
@Composable
fun AfternoteFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AfternoteFabSize,
    iconSize: Dp = 24.dp,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        containerColor = AfternoteDesign.colors.gray9,
        contentColor = AfternoteDesign.colors.white,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.core_ui_circle_button_plus),
            contentDescription = stringResource(R.string.core_ui_fab_content_description_add),
            modifier = Modifier.size(iconSize),
        )
    }
}

/** FAB 기본 지름. M3 [FloatingActionButton] 기본값과 같다. */
val AfternoteFabSize: Dp = 56.dp

/** M3 `Scaffold` 가 FAB 과 화면 가장자리 사이에 두는 기본 여백. */
private val FabScreenMargin: Dp = 16.dp

/**
 * FAB 이 뜬 화면의 목록이 **마지막 항목을 가리지 않도록** 남겨야 할 하단 여백.
 *
 * `Scaffold` 의 `paddingValues` 는 FAB 자리를 예약하지 않는다 — FAB 은 콘텐츠 위에 뜨는 것이
 * 설계다. 그래서 목록이 스스로 이만큼을 비워야 한다. 항목이 많아 스크롤이 생기면 밀어서 볼
 * 수라도 있지만, **항목이 한둘이라 스크롤 자체가 없으면 가려진 채 고정된다** — 기록이 적은
 * 신규 사용자가 정확히 그 상태다.
 *
 * FAB 지름에 위아래 기본 여백(M3 `Scaffold` 가 FAB 에 주는 16dp)을 더한 값이라, 지름이
 * 바뀌면 여백도 함께 따라온다 — 두 값이 따로 놀지 않게 한 자리에서 낸다.
 */
val AfternoteFabContentBottomPadding: Dp = AfternoteFabSize + FabScreenMargin * 2
