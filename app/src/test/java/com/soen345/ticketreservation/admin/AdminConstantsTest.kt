package com.soen345.ticketreservation.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminConstantsTest {

    @Test
    fun `test ADMIN_CODE is correct`() {
        assertEquals("SOEN345ADMIN", AdminConstants.ADMIN_CODE)
    }
}
