package com.afternote.core.common.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationDestinationTest {
    @Test
    fun `계약값으로 목적지를 되찾는다`() {
        NotificationDestination.entries.forEach { destination ->
            assertEquals(
                destination,
                NotificationDestination.fromContractValue(destination.contractValue),
            )
        }
    }

    @Test
    fun `계약 밖 값과 빈 값은 목적지가 아니다`() {
        listOf(null, "", "   ", "HOME", "unknown", "home2").forEach { raw ->
            assertNull(NotificationDestination.fromContractValue(raw))
        }
    }

    @Test
    fun `앞뒤 공백은 다듬어 해석한다`() {
        assertEquals(
            NotificationDestination.HOME,
            NotificationDestination.fromContractValue("  home\n"),
        )
    }

    @Test
    fun `계약값은 서로 다르고 비어 있지 않다`() {
        val contractValues = NotificationDestination.entries.map(NotificationDestination::contractValue)

        assertEquals(contractValues.size, contractValues.toSet().size)
        assertEquals(emptyList<String>(), contractValues.filter(String::isBlank))
    }
}
