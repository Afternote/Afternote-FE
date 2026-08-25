package com.afternote.core.ui.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R

/** 프로필 아이콘 앵커. 장식이라 semantics 이름이 없어 테스트가 이 태그로 존재 여부를 본다. */
const val PROFILE_ICON_TEST_TAG = "home_top_bar_profile"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    showProfileIcon: Boolean = true,
    onSettingClick: () -> Unit = {},
) {
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(com.afternote.core.common.R.drawable.core_common_logo),
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(start = 25.dp)
                        .size(90.dp),
            )
        },
        title = { },
        actions = {
            Row(
                modifier =
                    Modifier
                        .padding(end = 25.dp),
            ) {
                if (showProfileIcon) {
                    // 목적지가 없어 어디서도 눌리지 않는다 — 장식이므로 이름을 붙이지 않는다.
                    // 이름을 주면 TalkBack 이 포커스 가능한 노드로 읽어, 사용자가 액션이 있다고
                    // 믿고 탭했다가 아무 반응도 얻지 못한다 (#613 리뷰).
                    Image(
                        painter = painterResource(R.drawable.core_ui_user),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).testTag(PROFILE_ICON_TEST_TAG),
                    )

                    Spacer(modifier = Modifier.width(15.dp))
                }

                Image(
                    painter = painterResource(R.drawable.core_ui_settings),
                    // 유일하게 눌리는 액션인데 접근성 트리에 이름이 없었다 — 스크린리더가
                    // "버튼" 으로만 읽는다 (#613).
                    contentDescription = stringResource(R.string.core_ui_home_top_bar_setting),
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button) { onSettingClick() },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    HomeTopBar()
}
