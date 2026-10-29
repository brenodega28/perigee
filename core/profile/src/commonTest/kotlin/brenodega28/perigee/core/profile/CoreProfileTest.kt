package brenodega28.perigee.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreProfileTest {
    @Test
    fun nameIsStable() {
        assertEquals("core-profile", CoreProfile.NAME)
    }
}
