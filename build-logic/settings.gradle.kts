// -----------------------------------------------------------------------------
// build-logic 프로젝트 루트 설정
// -----------------------------------------------------------------------------

// 외부에서 includeBuild로 호출되는 빌드 로직 프로젝트의 이름을 명시합니다.
// 이름을 지정하지 않으면 Gradle이 폴더 경로에 따라 임의의 이름을 생성하여
// 빌드 캐시가 깨지는 현상이 발생할 수 있습니다. (경고 3번 해결)
rootProject.name = "build-logic"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
