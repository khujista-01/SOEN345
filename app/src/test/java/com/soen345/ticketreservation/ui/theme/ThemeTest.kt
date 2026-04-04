package com.soen345.ticketreservation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test light theme is applied`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = false, dynamicColor = false) {
                Text("Theme Test")
            }
        }
        composeTestRule.onNodeWithText("Theme Test").assertExists()
    }

    @Test
    fun `test dark theme is applied`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = true, dynamicColor = false) {
                Text("Theme Test")
            }
        }
        composeTestRule.onNodeWithText("Theme Test").assertExists()
    }
}
