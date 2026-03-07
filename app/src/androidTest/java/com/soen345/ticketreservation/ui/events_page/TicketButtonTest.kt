package com.soen345.ticketreservation.ui.events_page

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class TicketButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ticketButton_togglesReservation() {
        composeTestRule.setContent {

            var reserved by remember { mutableStateOf(false) }

            TicketButton(
                isReserved = reserved,
                onReservedChange = { reserved = it }
            )
        }

        composeTestRule
            .onNodeWithText("Reserve Ticket")
            .performClick()

        composeTestRule
            .onNodeWithText("Cancel Reservation")
            .assertExists()
    }
}