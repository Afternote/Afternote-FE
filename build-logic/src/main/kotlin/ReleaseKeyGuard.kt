import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * release variant 빌드 시 [value]가 빈 값이면 명확한 메시지로 빌드를 실패시킨다.
 *
 * `localProperties.getProperty(...) ?: System.getenv(...) ?: ""` 폴백은 키가 없어도 빌드를
 * 통과시켜, 소셜 로그인이 불능인 release 산출물을 조용히 만들어낸다. 검증 태스크를
 * `preReleaseBuild` 앞에 걸어 release variant 를 실제로 빌드하는 경우에만 실패시키고,
 * debug 빌드는 기존처럼 빈 값을 허용한다(로컬 개발 편의).
 */
fun Project.requireKeyForReleaseBuild(
    keyName: String,
    value: String,
) {
    val taskSuffix =
        keyName.split("_").joinToString("") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    val guard =
        tasks.register("check${taskSuffix}ForRelease") {
            group = "verification"
            description = "release 빌드 전에 $keyName 주입 여부를 검증한다."
            // configuration cache 가 태스크 상태를 직렬화하므로 Project 참조 없이 값만 캡처한다.
            val isMissing = value.isBlank()
            doFirst {
                if (isMissing) {
                    throw GradleException(
                        """
                        |$keyName 가 비어 있어 release 빌드를 중단합니다.
                        |빈 키로 빌드된 release APK 는 소셜 로그인이 동작하지 않습니다.
                        |루트 local.properties 또는 CI 환경변수 $keyName 를 설정한 뒤 다시 빌드하세요.
                        |발급·설정 방법은 README '신규 팀원 빌드 셋업' 섹션 참고.
                        """.trimMargin(),
                    )
                }
            }
        }
    tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(guard) }
}
