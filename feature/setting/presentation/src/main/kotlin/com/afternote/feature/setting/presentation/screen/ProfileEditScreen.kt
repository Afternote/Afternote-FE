package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "프로필 수정",
                onBackClick = onBackClick,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding), // ← 이게 없으면 TopAppBar 뒤로 content가 숨어버림
        ) {
            item {
                Box(modifier = Modifier) {
                    Image(painterResource(R.drawable.ic_default_profile), contentDescription = "기본")
                    Image(painterResource(R.drawable.ic_plus), contentDescription = "추가")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPrev() {
    ProfileEditScreen(onBackClick = {})
}
