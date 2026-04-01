package com.soen345.ticketreservation.ui.events_page

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class TicketButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test button shows Reserve Ticket when not reserved`() {
        composeTestRule.setContent {
            TicketButton(isReserved = false, onReservedChange = {})
        }

        composeTestRule.onNodeWithText("Reserve Ticket").assertExists()
    }

    @Test
    fun `test button shows Cancel Reservation when reserved`() {
        composeTestRule.setContent {
            TicketButton(isReserved = true, onReservedChange = {})
        }

        composeTestRule.onNodeWithText("Cancel Reservation").assertExists()
    }

    @Test
    fun `test button click toggles state`() {
        var reserved = false
        composeTestRule.setContent {
            TicketButton(
                isReserved = reserved,
                onReservedChange = { reserved = it }
            )
        }

        composeTestRule.onNodeWithText("Reserve Ticket").performClick()
        assertTrue(reserved)
    }
}
