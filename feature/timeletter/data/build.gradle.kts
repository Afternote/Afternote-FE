plugins {
    id("afternote.android.data")
}

android {
    namespace = "com.afternote.feature.timeletter.data"
}

dependencies {
    implementation(projects.feature.timeletter.domain)
    implementation(projects.core.network)
}
