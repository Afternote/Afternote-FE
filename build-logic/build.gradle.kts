plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    alias(libs.plugins.ktlint)
}

// build-logic 자체도 누락된 visibility·반환 타입을 inventory 하되, 기존 경고를 한 번에 실패로
// 바꾸지는 않는다. 정리 후 strict 로 승격한다.
kotlin {
    explicitApiWarning()
}

ktlint {
    version.set(libs.versions.ktlint)
    filter {
        exclude { it.file.path.contains("build/") }
    }
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ktlint.gradle)
    implementation(libs.kover.gradlePlugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
    testImplementation(libs.junit)
    testImplementation(gradleTestKit())

    // 루트 build.gradle.kts 의 보안 하한은 별도 빌드인 여기까지 미치지 않는다 — 같은 근거(#921·#981·
    // #982·#985). AGP 9.3.2 이 이 클래스패스에도 같은 취약 버전을 끌어온다(netty 는 여기 없다).
    constraints {
        listOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on").forEach { artifact ->
            implementation("org.bouncycastle:$artifact:${libs.versions.bouncycastle.get()}") {
                because("GHSA-574f-3g2m-x479 등 1.84 미만 취약 — #921")
            }
        }
        implementation("org.apache.commons:commons-lang3:${libs.versions.commonsLang3.get()}") {
            because("GHSA-j288-q9x7-2f5v — 3.18.0 미만 취약 — #981")
        }
        implementation("org.bitbucket.b_c:jose4j:${libs.versions.jose4j.get()}") {
            because("GHSA-3677-xxcr-wjqv — 0.9.6 미만 취약 — #982")
        }
        implementation("org.jdom:jdom2:${libs.versions.jdom2.get()}") {
            because("GHSA-2363-cqg2-863c — 2.0.6.1 미만 취약 — #985")
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
        register("jvmLibrary") {
            id = "afternote.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("jvmDomain") {
            id = "afternote.jvm.domain"
            implementationClass = "JvmDomainConventionPlugin"
        }
        register("androidApplication") {
            id = "afternote.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLint") {
            id = "afternote.android.lint"
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("jvmLint") {
            id = "afternote.jvm.lint"
            implementationClass = "JvmLintConventionPlugin"
        }
        register("androidDatastore") {
            id = "afternote.android.datastore"
            implementationClass = "AndroidDatastoreConventionPlugin"
        }
        register("kover") {
            id = "afternote.kover"
            implementationClass = "AfternoteKoverConventionPlugin"
        }
    }
}
