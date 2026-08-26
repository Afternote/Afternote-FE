plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
    resourcePrefix = "mindrecord_"

    // Robolectric 이 컴파일된 리소스로 화면을 띄운다 (#729).
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(libs.coil.compose)
    // 쓰는 건 BasicRichTextEditor(foundation 계열)뿐인데, richeditor 가 딸려 보내는 Compose
    // Multiplatform material3 1.11.0-alpha07 이 androidx material3 를 BOM 판 1.4.0 대신
    // 1.5.0-alpha17(alpha)로 끌어올린다(#973). 쓰지 않는 material3 갈래를 끊어 BOM 을 정본으로 되돌린다.
    implementation(libs.compose.rich.editor) {
        exclude(group = "org.jetbrains.compose.material3", module = "material3")
    }

    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.feature.mindrecord.domain))

    // 컴파일된 리소스로 문구를 검증한다 — aapt2 의 앞뒤 공백 제거는 소스 XML 만 봐서는 잡히지 않는다 (#732).
    // 열린 주차 메뉴의 스크롤·최하단 선택도 JVM 에서 그대로 재현한다 (#729).
    // 매퍼가 android.util.Log 를 타 JVM 단위 테스트로는 돌지 않는다 (#751).
    // 부분 성공에서 어떤 문구가 나가는지도 컴포지션을 태워야 확인된다 (#725).
    // 수신자 기록 본문 시트도 JVM 에서 실제로 렌더해 확인한다 (#618).
    // 카드 미리보기의 대체 문자 제거도 HtmlCompat 파싱 결과라 실제 파서로 확인한다 (#549).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.compose.rich.editor)

    testImplementation(libs.coroutines.test)

    // 라우트 인자 해석이 Bundle 을 타므로 실제 Android 구현이 필요하다 (#582).
    testImplementation(libs.robolectric)
}
