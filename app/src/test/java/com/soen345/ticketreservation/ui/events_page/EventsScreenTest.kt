package com.soen345.ticketreservation.ui.events_page

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34], qualifiers = "w480dp-h800dp-any-240dpi")
@RunWith(RobolectricTestRunner::class)
class EventsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleEvents = listOf(
        Event("1", "Rock Concert", "Music event", "Music", "Montreal", "2023-12-01", 100, 50.0),
        Event("2", "Art Expo", "Painting display", "Art", "Laval", "2023-12-05", 50, 20.0)
    )

    @Test
    fun `test filtering by search text`() {
        composeTestRule.setContent {
            Surface {
                EventsScreen(
                    events = sampleEvents,
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com",
                    onReservationChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Rock Concert").assertExists()
        composeTestRule.onNodeWithText("Art Expo").assertExists()

        composeTestRule.onNodeWithText("Search").performTextInput("Rock")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rock Concert").assertExists()
        composeTestRule.onNodeWithText("Art Expo").assertDoesNotExist()
    }

    @Test
    fun `test filtering by location`() {
        composeTestRule.setContent {
            Surface {
                EventsScreen(
                    events = sampleEvents,
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com",
                    onReservationChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Location").performTextInput("Laval")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Art Expo").assertExists()
        composeTestRule.onNodeWithText("Rock Concert").assertDoesNotExist()
    }
}
