import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VersionCodeTest {
    @Test
    fun `override가 없으면 기존 versionCode를 유지한다`() {
        assertEquals(DEFAULT_AFTERNOTE_VERSION_CODE, resolveAfternoteVersionCode(null))
    }

    @Test
    fun `유효한 Play versionCode를 주입한다`() {
        assertEquals(101, resolveAfternoteVersionCode("101"))
        assertEquals(MAX_PLAY_VERSION_CODE, resolveAfternoteVersionCode("$MAX_PLAY_VERSION_CODE"))
    }

    @Test
    fun `비어 있거나 양의 10진 정수가 아닌 override는 실패한다`() {
        listOf("", "  ", "0", "-1", "+1", "1.5", "abc", "01").forEach { invalid ->
            assertThrows(GradleException::class.java) {
                resolveAfternoteVersionCode(invalid)
            }
        }
    }

    @Test
    fun `Google Play 최대 versionCode를 넘으면 실패한다`() {
        assertThrows(GradleException::class.java) {
            resolveAfternoteVersionCode("2100000001")
        }
    }
}
