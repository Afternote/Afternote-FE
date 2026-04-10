package com.afternote.afternote_fe.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug 빌드 전용 Mock 모드 토글 화면.
 * 홈 화면에 별도 런처 아이콘("Afternote DEV")으로 노출됩니다.
 * 릴리즈에서는 이 클래스가 컴파일되지 않습니다.
 */
@AndroidEntryPoint
class DebugMockSettingsActivity : ComponentActivity() {
    private val viewModel: DebugSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isEnabled by viewModel.isMockEnabled.collectAsStateWithLifecycle()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier =
                        Modifier
                            .systemBarsPadding()
                            .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Text(
                        text = "Debug 설정",
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Mock 모드",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text =
                                    if (isEnabled) "목업 데이터 사용 중" else "실서버 연결 중",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleMockMode() },
                        )
                    }

                    Text(
                        text = "변경 사항은 다음 API 호출부터 즉시 적용됩니다.\n앱을 재시작할 필요가 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
