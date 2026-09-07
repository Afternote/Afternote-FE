import org.gradle.api.GradleException

const val AFTERNOTE_VERSION_CODE_ENV = "AFTERNOTE_VERSION_CODE"
private const val DEFAULT_AFTERNOTE_VERSION_CODE = 1
private const val MAX_PLAY_VERSION_CODE = 2_100_000_000

/**
 * Keeps ordinary local/Firebase builds on the existing versionCode while allowing an approved Play
 * workflow to inject its preflight-validated, monotonically increasing value.
 */
fun resolveAfternoteVersionCode(rawValue: String?): Int {
    if (rawValue == null) return DEFAULT_AFTERNOTE_VERSION_CODE

    val value = rawValue.trim()
    if (!value.matches(Regex("[1-9][0-9]*"))) {
        throw GradleException(
            "$AFTERNOTE_VERSION_CODE_ENV 는 1 이상 $MAX_PLAY_VERSION_CODE 이하의 10진 정수여야 합니다.",
        )
    }

    val parsed = value.toLongOrNull()
    if (parsed == null || parsed > MAX_PLAY_VERSION_CODE) {
        throw GradleException(
            "$AFTERNOTE_VERSION_CODE_ENV 는 Google Play 최대값 $MAX_PLAY_VERSION_CODE 이하여야 합니다.",
        )
    }
    return parsed.toInt()
}
