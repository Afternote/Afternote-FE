import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

const val CI_COMMIT_SHA_ENV = "GITHUB_SHA"
const val SHORT_COMMIT_SHA_LENGTH = 7
private const val UNKNOWN_COMMIT_MARKER = "unknown"

/**
 * debug `versionName` 에 붙일 빌드 지문을 만든다 — `1.0+a3f91c2` · `1.0+a3f91c2-dirty` · `1.0+unknown`.
 *
 * `BuildConfig` 상수가 아니라 `versionName` 에 싣는 이유는 앱 **바깥에서** 읽혀야 하기 때문이다.
 * `adb shell dumpsys package <id>` 한 줄로 보이므로, 실기 QA 전후에 설치된 앱이 바뀌었는지
 * 대조할 수 있다(다른 세션이 그 사이 재설치한 사고 — #1135).
 *
 * `-dirty` 가 실질적으로 중요하다. 로컬 빌드는 커밋 안 된 변경 위에서 도는 경우가 많아 sha 만으로는
 * "그 커밋을 검증했다"가 성립하지 않는다. 커밋을 특정할 수 없을 때 접미사를 생략하지 않고
 * [UNKNOWN_COMMIT_MARKER] 를 남기는 것도 같은 이유다 — 접미사 없는 `1.0` 은 "sha 를 못 읽었다"와
 * "이 기능이 없던 빌드"를 구분해 주지 않아, 없는 근거를 만들 여지를 남긴다.
 */
private fun debugVersionNameSuffix(
    rawCommitSha: String?,
    hasUncommittedChanges: Boolean,
): String {
    val shortSha = shortCommitSha(rawCommitSha) ?: return "+$UNKNOWN_COMMIT_MARKER"
    return if (hasUncommittedChanges) "+$shortSha-dirty" else "+$shortSha"
}

/**
 * 커밋 sha 를 [SHORT_COMMIT_SHA_LENGTH] 자리로 정규화한다. 16진수가 아니거나 짧으면 null.
 *
 * 입력이 두 갈래라 정규화가 필요하다 — CI 의 `GITHUB_SHA` 도 로컬 `git rev-parse HEAD` 도 40자다.
 * QA 대장 파일명(`docs/qa/evidence/<full-head-sha>.json`)과 대조하려면 자릿수가 일정해야 한다.
 */
private fun shortCommitSha(rawCommitSha: String?): String? {
    val value = rawCommitSha?.trim()?.lowercase() ?: return null
    if (!value.matches(Regex("[0-9a-f]{$SHORT_COMMIT_SHA_LENGTH,40}"))) return null
    return value.take(SHORT_COMMIT_SHA_LENGTH)
}

/**
 * 빌드 환경에서 커밋 상태를 읽어 debug `versionName` 접미사를 만든다.
 *
 * configuration cache 제약 때문에 git 을 빌드 스크립트에서 직접 부를 수 없어 [ValueSource] 로 감싼다.
 */
fun Project.resolveDebugVersionNameSuffix(): String =
    providers
        .of(DebugVersionNameSuffixValueSource::class.java) {
            parameters.workingDirectory.set(rootProject.layout.projectDirectory)
            parameters.ciCommitSha.set(providers.environmentVariable(CI_COMMIT_SHA_ENV).orElse(""))
        }.get()

/**
 * git 을 호출해 [debugVersionNameSuffix] 를 계산한다.
 *
 * **읽은 것을 그대로 내보내지 않고 접미사까지 좁혀서 반환하는 것이 이 클래스의 요점이다.**
 * ValueSource 의 반환값은 configuration cache 입력으로 기록되므로, `git status --porcelain` 출력을
 * 그대로 내보내면 **편집 중인 파일 목록이 바뀔 때마다** configuration 이 다시 돈다. dirty 여부만
 * 접미사에 반영하면 `+a3f91c2-dirty` 로 값이 같아, 같은 커밋 위에서 파일을 이리저리 고치는 동안
 * 캐시가 유지된다(실측: 무변경 재빌드 0.65초 · 커밋이 바뀐 빌드만 재저장).
 *
 * CI 는 `GITHUB_SHA` 를 그대로 쓴다 — checkout 직후라 워킹트리가 커밋 그대로이고, git 프로세스도 아낀다.
 */
abstract class DebugVersionNameSuffixValueSource : ValueSource<String, DebugVersionNameSuffixValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val workingDirectory: DirectoryProperty

        /** CI 밖에서는 빈 문자열. */
        val ciCommitSha: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val ciCommitSha = parameters.ciCommitSha.getOrElse("")
        if (ciCommitSha.isNotBlank()) {
            return debugVersionNameSuffix(ciCommitSha, hasUncommittedChanges = false)
        }

        // git 이 없거나 저장소가 아니면 커밋을 특정할 수 없다.
        val head = git("rev-parse", "HEAD") ?: return debugVersionNameSuffix(null, hasUncommittedChanges = false)

        // untracked 도 dirty 로 센다. status 를 못 읽었을 때 dirty 로 두는 것과 같은 근거 — 거짓 clean 은
        // 검증하지 않은 코드에 QA 증거를 붙이게 하지만, 거짓 dirty 는 증거를 한 번 더 확인하게 할 뿐이다.
        val status = git("status", "--porcelain")
        return debugVersionNameSuffix(head, hasUncommittedChanges = status == null || status.isNotEmpty())
    }

    /** git 이 없거나(실행 실패) 저장소가 아니면(비0 종료) null — 호출부가 «알 수 없음» 으로 처리한다. */
    private fun git(vararg arguments: String): String? {
        val captured = ByteArrayOutputStream()
        val result =
            try {
                execOperations.exec {
                    workingDir = parameters.workingDirectory.get().asFile
                    commandLine(listOf("git", *arguments))
                    standardOutput = captured
                    errorOutput = ByteArrayOutputStream()
                    isIgnoreExitValue = true
                }
            } catch (_: Exception) {
                return null
            }
        if (result.exitValue != 0) return null
        return captured.toString(Charsets.UTF_8).trim()
    }
}
