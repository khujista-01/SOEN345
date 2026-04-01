package com.soen345.ticketreservation.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class EventCategoryTest {

    @Test
    fun `test EventCategory properties`() {
        val category = EventCategory(
            id = "cat1",
            name = "Music",
            description = "Live concerts and festivals"
        )

        assertEquals("cat1", category.id)
        assertEquals("Music", category.name)
        assertEquals("Live concerts and festivals", category.description)
    }

    @Test
    fun `test EventCategory default description`() {
        val category = EventCategory(id = "cat1", name = "Music")
        assertEquals("", category.description)
    }
}
