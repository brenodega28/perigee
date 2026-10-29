package brenodega28.perigee.probe.common

import kotlin.test.Test
import kotlin.test.assertEquals

class ProbeCommonTest {
    @Test
    fun nameIsStable() {
        assertEquals("probe-common", ProbeCommon.NAME)
    }
}
