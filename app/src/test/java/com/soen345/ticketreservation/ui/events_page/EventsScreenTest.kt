package com.soen345.ticketreservation.ui.events_page

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        Event("1", "Rock Concert", "Epic music night", "Music", "Montreal", "2023-12-01", 100, 50.0),
        Event("2", "Art Expo", "Painting display", "Art", "Laval", "2023-12-05", 50, 20.0),
        Event("3", "Jazz Fest", "Smooth jazz", "Music", "Montreal", "2023-12-10", 0, 15.0)
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
    fun `test all filtering logic combinations`() {
        composeTestRule.setContent {
            Surface {
                EventsScreen(
                    events = sampleEvents,
                    userId = "u1",
                    userAccessToken = "t1",
                    userEmail = "e1",
                    onReservationChanged = {}
                )
            }
        }

        // 1. Match description via search
        composeTestRule.onNodeWithText("Search").performTextInput("Epic")
        composeTestRule.onNodeWithText("Rock Concert").assertExists()
        composeTestRule.onNodeWithText("Art Expo").assertDoesNotExist()
        composeTestRule.onNodeWithText("Search").performTextClearance()

        // 2. Filter by Category
        composeTestRule.onNodeWithText("Category").performTextInput("Art")
        composeTestRule.onNodeWithText("Art Expo").assertExists()
        composeTestRule.onNodeWithText("Rock Concert").assertDoesNotExist()
        composeTestRule.onNodeWithText("Category").performTextClearance()

        // 3. Filter by Date
        composeTestRule.onNodeWithText("Date").performTextInput("2023-12-10")
        composeTestRule.onNodeWithText("Jazz Fest").assertExists()
        composeTestRule.onNodeWithText("Rock Concert").assertDoesNotExist()
        composeTestRule.onNodeWithText("Date").performTextClearance()

        // 4. Combined Filter (Location + Category) - multiple matches
        composeTestRule.onNodeWithText("Location").performTextInput("Montreal")
        composeTestRule.onNodeWithText("Category").performTextInput("Music")
        composeTestRule.onNodeWithText("Rock Concert").assertExists()
        composeTestRule.onNodeWithText("Jazz Fest").assertExists()
        
        // 5. Filter with no matches
        composeTestRule.onNodeWithText("Search").performTextInput("NonExistent")
        composeTestRule.onNodeWithText("Rock Concert").assertDoesNotExist()
    }

    @Test
    fun `test event card interaction branches - reserve success`() {
        coEvery { SupabaseClient.insertReservation(any(), any(), any()) } returns true
        coEvery { SupabaseClient.sendConfirmationEmail(any(), any(), any(), any()) } returns (200 to "OK")

        composeTestRule.setContent {
            Surface {
                var isReserved by remember { mutableStateOf(false) }
                EventCard(
                    event = sampleEvents[0],
                    isReserved = isReserved,
                    userId = "u1",
                    userAccessToken = "t1",
                    userEmail = "e1",
                    onReservedStateChanged = { isReserved = it },
                    onReservationChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Reserve Ticket").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirmation").assertExists()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Cancel Reservation").assertExists()
    }

    @Test
    fun `test event card interaction branches - cancel failure`() {
        coEvery { SupabaseClient.deleteReservation(any(), any(), any()) } returns false

        composeTestRule.setContent {
            Surface {
                var isReserved by remember { mutableStateOf(true) }
                EventCard(
                    event = sampleEvents[1].copy(isReservedByCurrentUser = true),
                    isReserved = isReserved,
                    userId = "u1",
                    userAccessToken = "t1",
                    userEmail = "e1",
                    onReservedStateChanged = { isReserved = it },
                    onReservationChanged = {}
                )
            }
        }

        // Branch: Cancel Failure (should log error and not change state)
        composeTestRule.onNodeWithText("Cancel Reservation").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel Reservation").assertExists()
    }

    @Test
    fun `test event card toggles reserve cancel reserve around confirmation dialog`() {
        coEvery { SupabaseClient.insertReservation(any(), any(), any()) } returns true
        coEvery { SupabaseClient.deleteReservation(any(), any(), any()) } returns true
        coEvery { SupabaseClient.sendConfirmationEmail(any(), any(), any(), any()) } returns (200 to "OK")

        composeTestRule.setContent {
            Surface {
                var isReserved by remember { mutableStateOf(false) }
                EventCard(
                    event = sampleEvents[0],
                    isReserved = isReserved,
                    userId = "u1",
                    userAccessToken = "t1",
                    userEmail = "e1",
                    onReservedStateChanged = { isReserved = it },
                    onReservationChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Reserve Ticket").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Confirmation").assertExists()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithText("Cancel Reservation").assertExists()

        composeTestRule.onNodeWithText("Cancel Reservation").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reserve Ticket").assertExists()
    }

    @Test
    fun `test loading state`() {
        coEvery { SupabaseClient.fetchEvents(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(10000)
            SupabaseClient.FetchEventsResult(emptyList(), null)
        }
        
        composeTestRule.setContent { 
            Surface {
                EventsLoadingScreen("u1", "t1", "e1") 
            }
        }
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun `test error state`() {
        coEvery { SupabaseClient.fetchEvents(any(), any()) } returns SupabaseClient.FetchEventsResult(null, "Network Error")
        
        composeTestRule.setContent { 
            Surface {
                EventsLoadingScreen("u1", "t1", "e1") 
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Network Error").assertExists()
    }
}
