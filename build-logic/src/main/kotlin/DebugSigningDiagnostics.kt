import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64

/**
 * debug 서명 폴백을 매 빌드에 알리고, 실제 서명 인증서의 카카오 키 해시를 제공한다.
 *
 * configuration cache 재사용 시에도 경고가 남도록 태스크 액션에서 출력한다.
 * 액션에는 AGP DSL이나 Project 대신 확정된 서명 값만 캡처한다.
 */
public fun Project.registerDebugSigningDiagnostics(
    usingDefaultKeystore: Boolean,
    storeFile: File?,
    storePassword: String?,
    keyAlias: String?,
) {
    val warning =
        tasks.register("checkDebugSigningForKakao") {
            group = "verification"
            description = "기본 debug 서명으로 카카오톡 로그인 QA가 제한될 수 있음을 알린다."
            doLast {
                if (usingDefaultKeystore) {
                    logger.warn(
                        """
                        |DEBUG_* 서명 설정이 없어 AGP 기본 debug keystore로 폴백합니다.
                        |이 키 해시가 카카오 콘솔에 등록되지 않았다면 카카오톡 앱 로그인에 실패하고 웹 로그인으로 폴백할 수 있습니다.
                        |웹 로그인 성공만으로 카카오톡 앱-투-앱 로그인 QA를 완료하지 마세요.
                        |README '공유 debug keystore'를 따라 설정하거나, ./gradlew :app:printDebugKakaoKeyHash 출력값을 콘솔에 등록하세요.
                        """.trimMargin(),
                    )
                }
            }
        }
    tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(warning) }

    tasks.register("printDebugKakaoKeyHash") {
        group = "help"
        description = "실제 debug 서명 인증서의 카카오 키 해시(Base64 SHA-1)를 출력한다."
        // 새 머신에서는 AGP가 기본 debug keystore를 먼저 생성하도록 한다.
        dependsOn("validateSigningDebug")
        doLast {
            logger.lifecycle("Debug Kakao key hash: ${debugKakaoKeyHash(storeFile, storePassword, keyAlias)}")
        }
    }
}

private fun debugKakaoKeyHash(
    storeFile: File?,
    storePassword: String?,
    keyAlias: String?,
): String {
    if (storeFile?.isFile != true || storePassword == null || keyAlias.isNullOrBlank()) {
        throw GradleException("debug 서명 설정과 keystore 파일을 확인하세요. README '공유 debug keystore' 참고.")
    }
    // 파일 형식을 탐지해 AGP 기본 JKS와 팀 공유 PKCS12 keystore를 모두 지원한다.
    val keyStore = KeyStore.getInstance(storeFile, storePassword.toCharArray())
    val certificate =
        keyStore.getCertificate(keyAlias)
            ?: throw GradleException("debug 서명 alias의 인증서를 찾을 수 없습니다. DEBUG_KEY_ALIAS를 확인하세요.")
    return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(certificate.encoded))
}
