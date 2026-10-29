package brenodega28.perigee.core.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreProtocolTest {
    @Test
    fun nameIsStable() {
        assertEquals("core-protocol", CoreProtocol.NAME)
    }
}
