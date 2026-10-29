package brenodega28.perigee.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreModelTest {
    @Test
    fun nameIsStable() {
        assertEquals("core-model", CoreModel.NAME)
    }
}
