plugins {
    id("afternote.android.domain")
}

android {
    namespace = "com.afternote.feature.timeletter.domain"
}

dependencies {
    implementation(projects.core.domain)
}
