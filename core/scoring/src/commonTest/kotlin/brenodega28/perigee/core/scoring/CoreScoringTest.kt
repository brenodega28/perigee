package brenodega28.perigee.core.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreScoringTest {
    @Test
    fun nameIsStable() {
        assertEquals("core-scoring", CoreScoring.NAME)
    }
}
