package com.soen345.ticketreservation.ui.events_page

import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.soen345.ticketreservation.data.SupabaseClient
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
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

    @Before
    fun setUp() {
        mockkObject(SupabaseClient)
    }

    @After
    fun tearDown() {
        unmockkObject(SupabaseClient)
    }

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

    @Test
    fun `test loading state shows progress indicator`() {
        // We don't complete the coEvery so it stays in loading state
        coEvery { SupabaseClient.fetchEvents(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(5000)
            SupabaseClient.FetchEventsResult(emptyList(), null)
        }

        composeTestRule.setContent {
            Surface {
                EventsLoadingScreen(
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com"
                )
            }
        }

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun `test error state shows error message`() {
        coEvery { SupabaseClient.fetchEvents(any(), any()) } returns 
            SupabaseClient.FetchEventsResult(null, "Network Error")

        composeTestRule.setContent {
            Surface {
                EventsLoadingScreen(
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com"
                )
            }
        }

        composeTestRule.onNodeWithText("Network Error").assertExists()
    }

    @Test
    fun `test empty list state`() {
        coEvery { SupabaseClient.fetchEvents(any(), any()) } returns 
            SupabaseClient.FetchEventsResult(emptyList(), null)

        composeTestRule.setContent {
            Surface {
                EventsLoadingScreen(
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com"
                )
            }
        }

        // Search field exists, but no EventCards exist
        composeTestRule.onNodeWithText("Search").assertExists()
        // Check that no "Location:" text exists (which would be inside an EventCard)
        composeTestRule.onNodeWithText("Location:", substring = true).assertDoesNotExist()
    }

    @Test
    fun `test reservation success shows confirmation dialog`() {
        coEvery { SupabaseClient.insertReservation(any(), any(), any()) } returns true
        coEvery { SupabaseClient.sendConfirmationEmail(any(), any(), any(), any()) } returns (200 to "Success")

        composeTestRule.setContent {
            Surface {
                EventCard(
                    event = sampleEvents[0],
                    userId = "user1",
                    userAccessToken = "token",
                    userEmail = "user@test.com",
                    onReservationChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Reserve Ticket").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Confirmation").assertExists()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Confirmation").assertDoesNotExist()
    }
}
