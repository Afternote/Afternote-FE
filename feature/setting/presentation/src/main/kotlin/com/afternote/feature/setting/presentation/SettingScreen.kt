package com.afternote.feature.setting.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 임시 설정 화면. 로그아웃 버튼 하나만 노출한다.
 * 정식 IA가 확정되면 이 컴포저블을 갈아끼운다.
 */
@Composable
fun SettingScreen(
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val logoutCompleted by viewModel.logoutCompleted.collectAsStateWithLifecycle()

    // 2. 람다를 최신 상태로 캐싱하는 래퍼 변수 생성
    val currentOnLogoutSuccess by rememberUpdatedState(onLogoutSuccess)

    LaunchedEffect(logoutCompleted) {
        if (logoutCompleted) {
            // 3. 파라미터가 아닌, 래핑된 변수를 호출!
            currentOnLogoutSuccess()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = viewModel::logout) {
            Text(text = "로그아웃")
        }
    }
}
