plugins {
    id("afternote.android.library.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.afternote.core.ui"
    resourcePrefix = "core_ui_"
}
