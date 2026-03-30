package com.soen345.ticketreservation.ui.events_page

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class EventsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eventsScreen_displays_events_and_buttons() {
        val events = listOf(
            Event(
                id = "1",
                title = "Concert",
                description = "Fun",
                category = "Music",
                location = "Hall A",
                date = "2026-03-10",
                availableTickets = 50,
                price = 30.0
            )
        )

        composeRule.setContent {
            EventsScreen(
                events = events,
                userId = "user1",
                userAccessToken = "token",
                userEmail =  "nicoleantounn@hotmail.com",
                )
        }

        composeRule.onNodeWithText("Concert").assertExists()
        composeRule.onNodeWithText("Reserve Ticket").assertExists()
    }
}