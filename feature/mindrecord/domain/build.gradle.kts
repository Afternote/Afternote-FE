plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.feature.mindrecord.domain"
}

dependencies {
    implementation(libs.coroutines.core)

    testImplementation(libs.coroutines.test)
}
