package com.soen345.ticketreservation.ui.admin

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.soen345.ticketreservation.admin.AdminEventManager
import com.soen345.ticketreservation.data.SupabaseClient
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34], qualifiers = "w480dp-h800dp-any-240dpi")
@RunWith(RobolectricTestRunner::class)
class AdminEventScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        mockkObject(SupabaseClient)
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(emptyList(), null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test adding an event shows success message`() {
        coEvery { SupabaseClient.insertAdminEvent(any(), any()) } returns Pair(true, "Success")
        
        composeTestRule.setContent {
            Surface {
                AdminEventScreen(
                    manager = AdminEventManager(),
                    accessToken = "test_token",
                    onBackToNormal = {}
                )
            }
        }

        composeTestRule.onNodeWithText("id").performTextInput("123")
        composeTestRule.onNodeWithText("title").performTextInput("Test Event")
        composeTestRule.onNodeWithText("price").performTextInput("10.0")
        composeTestRule.onNodeWithText("availableTickets").performTextInput("50")

        composeTestRule.onNodeWithText("Add Event").performClick()
        composeTestRule.waitForIdle()

        // Using assertExists() is safer in Robolectric than assertIsDisplayed()
        composeTestRule.onNodeWithText("Event added successfully.", substring = true).assertExists()
    }

    @Test
    fun `test error message on invalid input`() {
        composeTestRule.setContent {
            Surface {
                AdminEventScreen(
                    manager = AdminEventManager(),
                    accessToken = "test_token",
                    onBackToNormal = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add Event").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Price must be a valid number", substring = true).assertExists()
    }

    @Test
    fun `test cancelling event requires ID`() {
        composeTestRule.setContent {
            Surface {
                AdminEventScreen(
                    manager = AdminEventManager(),
                    accessToken = "test_token",
                    onBackToNormal = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel Event").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Event ID is required to cancel", substring = true).assertExists()
    }
}
