plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktlint.gradle)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    testImplementation(libs.junit)
    testImplementation(gradleTestKit())

    // 루트 build.gradle.kts 의 bouncycastle 하한은 별도 빌드인 여기까지 미치지 않는다 — 같은 근거(#921).
    constraints {
        listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach { artifact ->
            implementation("org.bouncycastle:$artifact:${libs.versions.bouncycastle.get()}") {
                because("GHSA-574f-3g2m-x479 등 1.84 미만 취약 — #921")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    // TestKit 스텁 프로젝트가 buildscript classpath 로 주입할 가드 클래스 위치 (ReleaseKeyGuardTest 참고).
    systemProperty(
        "guardClasspath",
        sourceSets.main
            .get()
            .output.classesDirs.asPath,
    )
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "afternote.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "afternote.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "afternote.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidRetrofit") {
            id = "afternote.android.retrofit"
            implementationClass = "AndroidRetrofitConventionPlugin"
        }
        register("androidNavigation") {
            id = "afternote.android.navigation"
            implementationClass = "AndroidNavigationConventionPlugin"
        }
        register("androidFeature") {
            id = "afternote.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidData") {
            id = "afternote.android.data"
            implementationClass = "AndroidDataConventionPlugin"
        }
        register("androidDomain") {
            id = "afternote.android.domain"
            implementationClass = "AndroidDomainConventionPlugin"
        }
        register("androidApplication") {
            id = "afternote.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLint") {
            id = "afternote.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("androidDatastore") {
            id = "afternote.android.datastore"
            implementationClass = "AndroidDatastoreConventionPlugin"
        }
    }
}
