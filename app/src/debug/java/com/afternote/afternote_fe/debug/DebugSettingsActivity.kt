package com.afternote.afternote_fe.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.EXTRA_DEBUG_START_TIMELETTER
import com.afternote.afternote_fe.MainActivity
import com.afternote.afternote_fe.enableLightEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug 빌드 전용 개발 도구 화면.
 * 홈 화면에 별도 런처 아이콘("Afternote DEV")으로 노출됩니다.
 * 릴리즈에서는 이 클래스가 컴파일되지 않습니다.
 */
@AndroidEntryPoint
class DebugSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        setContent {
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

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            startActivity(
                                Intent(this@DebugSettingsActivity, MainActivity::class.java).apply {
                                    putExtra(EXTRA_DEBUG_START_TIMELETTER, true)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                },
                            )
                        },
                    ) {
                        Text("타임레터로 이동 (로그인 스킵)")
                    }
                }
            }
        }
    }
}
