package com.soen345.ticketreservation.ui.events_page

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class EventsDummyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eventsDummyScreen_renders_mock_events() {
        val mockEvents = listOf(
            Event("1", "Rock Concert", "Live Music", "Music", "Montreal", "2023-12-01", 100, 50.0),
            Event("2", "Art Expo", "Painting display", "Art", "Laval", "2023-12-05", 50, 20.0),
            Event("3", "Tech Talk", "AI and Future", "Tech", "Montreal", "2023-12-10", 30, 0.0)
        )

        composeRule.setContent {
            EventsScreen(
                events = mockEvents,
                userId = "user1",
                userAccessToken = "token",
                userEmail = "test@test.com",
                onReservationChanged = {}
            )
        }

        composeRule.onNodeWithText("Rock Concert").assertExists()
        composeRule.onAllNodesWithText("Reserve Ticket").assertCountEquals(3)
    }

}
