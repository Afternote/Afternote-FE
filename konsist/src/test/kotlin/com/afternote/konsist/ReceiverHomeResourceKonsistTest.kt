package com.afternote.konsist

import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

/**
 * 수신자 홈이 형제 feature 의 리소스를 빌려 쓰지 않는지 (#926).
 *
 * 종전에는 수신자 홈이 `feature:afternote:presentation` 의 `R` 을 직접 import 해 소셜 패턴
 * 아이콘을 그렸다. 리소스는 모듈 경계를 가장 조용히 넘는 의존이다 — 컴파일은 되고,
 * 그림도 뜨고, 다른 모듈이 리소스를 지우거나 이름을 바꿀 때에야 드러난다.
 *
 * 아이콘의 정본은 `core:ui` 이고, 화면 사이에 오가는 값은 res id 가 아니라
 * `AfternoteSourceIcon` 이다.
 */
class ReceiverHomeResourceKonsistTest {
    @Test
    fun `수신자 홈은 다른 feature 의 R 을 참조하지 않는다`() {
        AfternoteKonsistScope
            .files
            .withPackage("com.afternote.feature.home.presentation.receiver..")
            .assertFalse { file ->
                file.imports.any { import -> FOREIGN_FEATURE_RESOURCE.matches(import.name) } ||
                    // import 만 보면 FQN 참조가 그대로 빠져나간다 — 이 패키지의 screenshotTest 가
                    // 실제로 `com.afternote.core.ui.R.drawable.…` 를 FQN 으로 쓰는 손버릇이라,
                    // 같은 손버릇으로 형제 feature 의 R 을 쓰면 가드가 침묵한다. 본문도 본다.
                    FOREIGN_FEATURE_RESOURCE_REFERENCE.containsMatchIn(file.text)
            }
    }

    private companion object {
        /** 수신자 홈이 사는 `home` 모듈이 아닌 feature 의 `R` (또는 그 하위 참조). #1462 로 모듈이 바뀌었다. */
        val FOREIGN_FEATURE_RESOURCE =
            Regex("""^com\.afternote\.feature\.(?!home\.)[a-z]+\.presentation\.R(\..*)?$""")

        /** 같은 참조를 본문 어디서든 — FQN 으로 쓴 자리를 잡는다. 주석·문자열 오탐은 감수한다. */
        val FOREIGN_FEATURE_RESOURCE_REFERENCE =
            Regex("""com\.afternote\.feature\.(?!home\.)[a-z]+\.presentation\.R\.""")
    }
}
