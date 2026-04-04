package com.soen345.ticketreservation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.soen345.ticketreservation.data.AuthClient
import com.soen345.ticketreservation.data.SupabaseClient
import com.soen345.ticketreservation.ui.theme.TicketReservationTheme
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        mockkObject(AuthClient)
        mockkObject(SupabaseClient)
        
        // Mock default behavior for Supabase fetches to avoid crashes
        coEvery { SupabaseClient.fetchEvents(any(), any()) } returns SupabaseClient.FetchEventsResult(emptyList(), null)
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(emptyList(), null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test app starts with Login screen`() {
        composeTestRule.setContent {
            TicketReservationTheme(dynamicColor = false) {
                AppRoot()
            }
        }

        composeTestRule.onNodeWithTag("login_card").assertExists()
        composeTestRule.onNodeWithText("Welcome back").assertExists()
    }

    @Test
    fun `test switching to Register tab`() {
        composeTestRule.setContent {
            TicketReservationTheme(dynamicColor = false) {
                AppRoot()
            }
        }

        composeTestRule.onNodeWithTag("register_tab").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("register_card").assertExists()
        composeTestRule.onNodeWithText("Create an account").assertExists()
    }

}
