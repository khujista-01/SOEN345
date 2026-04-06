package com.soen345.ticketreservation.ui.theme

import org.junit.Assert.assertNotNull
import org.junit.Test

class TypeTest {
    @Test
    fun `test typography is initialized`() {
        assertNotNull(Typography)
        assertNotNull(Typography.bodyLarge)
    }
}
