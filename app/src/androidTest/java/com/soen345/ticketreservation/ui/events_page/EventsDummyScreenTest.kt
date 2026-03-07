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
        composeRule.setContent {
            EventsDummyScreen(
                userId = "user1",
                userAccessToken = "token"
            )
        }

        composeRule.onNodeWithText("Rock Concert").assertExists()
        composeRule.onAllNodesWithText("Reserve Ticket").assertCountEquals(3)
    }

}