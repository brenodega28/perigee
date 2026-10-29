package brenodega28.perigee.core.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreRulesTest {
    @Test
    fun nameIsStable() {
        assertEquals("core-rules", CoreRules.NAME)
    }
}
