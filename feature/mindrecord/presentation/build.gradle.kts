plugins {
    id("afternote.android.library.compose")
    id("afternote.android.hilt")
    id("afternote.android.navigation")
    alias(libs.plugins.compose.screenshot)
    id("afternote.kover")
}

android {
    namespace = "com.afternote.feature.mindrecord.presentation"
    resourcePrefix = "mindrecord_"

    // Robolectric 이 컴파일된 리소스로 화면을 띄운다 (#729).
    testOptions.unitTests.isIncludeAndroidResources = true

    // 스크롤이 없는 화면은 세로가 모자라면 그대로 잘린다 — 좁은 화면 회귀를 CI 가 잡게 한다 (#1131).
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// 쓰는 건 BasicRichTextEditor(foundation 계열)뿐인데, richeditor 가 딸려 보내는 Compose Multiplatform
// material3 1.11.0-alpha07 이 androidx material3 를 BOM 판 1.4.0 대신 1.5.0-alpha17(alpha)로 끌어올린다
// (#973). 쓰지 않는 material3 갈래를 끊어 BOM 을 정본으로 되돌린다.
//
// #973 은 이 배제를 implementation(libs.compose.rich.editor) { exclude(...) } 로 «선언 한 줄»에 달았는데,
// 배제는 선언마다 따로 적용된다 — 병합이 되살린 배제 없는 중복 선언 한 줄이 알파 갈래를 그대로 다시 열어
// 같은 문제가 재발했다(#1654). 선언이 몇 줄이든 닫히도록 configuration 단위로 끊는다.
configurations.configureEach {
    exclude(group = "org.jetbrains.compose.material3", module = "material3")
}

dependencies {
    implementation(projects.feature.mindrecord.domain)
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.ui)
    implementation(projects.core.model)
    implementation(libs.coil.compose)
    // 첨부 이미지의 EXIF Orientation 을 읽어 본문 표시 크기를 세운다 (#731 리뷰).
    implementation(libs.androidx.exifinterface)
    implementation(libs.compose.rich.editor)

    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.domain))
    // 클릭 타깃 접근성 스캐너 (#1167). 크기·이름·역할(#1179)과 중첩 클릭(#1669) 계약을 여기서 지킨다.
    testImplementation(testFixtures(projects.core.ui))
    testImplementation(testFixtures(projects.feature.mindrecord.domain))

    // 캘린더 날짜 셀 상호작용을 JVM 에서 실제로 눌러 확인한다 (#724).
    // 부분 성공에서 어떤 문구가 나가는지도 컴포지션을 태워야 확인된다 (#725).
    // 첨부 파일명 해석은 ContentResolver 를 타므로 Robolectric 으로 검증한다 (#731).
    testImplementation(libs.androidx.test.core.ktx)
    // 컴파일된 리소스로 문구를 검증한다 — aapt2 의 앞뒤 공백 제거는 소스 XML 만 봐서는 잡히지 않는다 (#732).
    // 열린 주차 메뉴의 스크롤·최하단 선택도 JVM 에서 그대로 재현한다 (#729).
    // 매퍼가 android.util.Log 를 타 JVM 단위 테스트로는 돌지 않는다 (#751).
    // 부분 성공에서 어떤 문구가 나가는지도 컴포지션을 태워야 확인된다 (#725).
    // 수신자 기록 본문 시트도 JVM 에서 실제로 렌더해 확인한다 (#618).
    // 카드 미리보기의 대체 문자 제거도 HtmlCompat 파싱 결과라 실제 파서로 확인한다 (#549).
    // 라우트 인자 해석이 Bundle 을 타므로 실제 Android 구현이 필요하다 (#582).
    testImplementation(libs.robolectric)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
