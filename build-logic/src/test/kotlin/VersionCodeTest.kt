import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `versionCode` 주입 계약의 회귀 기준.
 *
 * 기대값을 프로덕션 상수로 쓰지 않고 리터럴로 적는다 (#1671). 상수끼리 비교하면 값이 바뀌어도
 * 테스트는 계속 통과해 «Play 상한이 2_100_000_000 이다» 라는 계약을 아무도 지키지 않는다.
 * 여기 적힌 숫자가 바뀌어야 할 때는 계약이 바뀐 때뿐이다.
 */
class VersionCodeTest {
    @Test
    fun `override가 없으면 기존 versionCode를 유지한다`() {
        assertEquals(1, resolveAfternoteVersionCode(null))
    }

    @Test
    fun `유효한 Play versionCode를 주입한다`() {
        assertEquals(101, resolveAfternoteVersionCode("101"))
        assertEquals(2_100_000_000, resolveAfternoteVersionCode("2100000000"))
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
