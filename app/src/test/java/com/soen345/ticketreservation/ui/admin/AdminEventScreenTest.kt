package com.soen345.ticketreservation.ui.admin

import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.soen345.ticketreservation.admin.AdminEvent
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
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test buildEventFromForm validation branches`() {
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(emptyList(), null)
        
        composeTestRule.setContent {
            Surface {
                AdminEventScreen(AdminEventManager(), "token", {})
            }
        }

        // 1. Invalid Price branch
        composeTestRule.onNodeWithText("price").performTextInput("invalid")
        composeTestRule.onNodeWithText("Add Event").performClick()
        composeTestRule.onNodeWithText("Price must be a valid number", substring = true).assertExists()

        // 2. Invalid Tickets branch
        composeTestRule.onNodeWithText("price").performTextReplacement("10.0")
        composeTestRule.onNodeWithText("availableTickets").performTextInput("abc")
        composeTestRule.onNodeWithText("Add Event").performClick()
        composeTestRule.onNodeWithText("Available tickets must be a valid integer", substring = true).assertExists()
    }

    @Test
    fun `test add and edit failure branches`() {
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(emptyList(), null)
        coEvery { SupabaseClient.insertAdminEvent(any(), any()) } returns Pair(false, "DB Error")
        coEvery { SupabaseClient.updateAdminEvent(any(), any()) } returns Pair(false, "Update Failed")

        composeTestRule.setContent {
            Surface {
                AdminEventScreen(AdminEventManager(), "token", {})
            }
        }

        // Fill form
        composeTestRule.onNodeWithText("price").performTextInput("10.0")
        composeTestRule.onNodeWithText("availableTickets").performTextInput("10")

        // Test Add Failure
        composeTestRule.onNodeWithText("Add Event").performClick()
        composeTestRule.onNodeWithText("DB Error", substring = true).assertExists()

        // Test Edit Failure
        composeTestRule.onNodeWithText("Edit Event").performClick()
        composeTestRule.onNodeWithText("Update Failed", substring = true).assertExists()
    }

    @Test
    fun `test cancel event logic branches`() {
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(emptyList(), null)
        coEvery { SupabaseClient.cancelAdminEvent(any(), any()) } returns Pair(true, "Cancelled")

        composeTestRule.setContent {
            Surface {
                AdminEventScreen(AdminEventManager(), "token", {})
            }
        }

        // 1. Missing ID branch
        composeTestRule.onNodeWithText("Cancel Event").performClick()
        composeTestRule.onNodeWithText("Event ID is required to cancel", substring = true).assertExists()

        // 2. Success branch
        composeTestRule.onNodeWithText("id").performTextInput("123")
        composeTestRule.onNodeWithText("Cancel Event").performClick()
        composeTestRule.onNodeWithText("Event cancelled successfully", substring = true).assertExists()
        
        // 3. Failure branch
        coEvery { SupabaseClient.cancelAdminEvent(any(), any()) } returns Pair(false, "Delete Forbidden")
        composeTestRule.onNodeWithText("Cancel Event").performClick()
        composeTestRule.onNodeWithText("Delete Forbidden", substring = true).assertExists()
    }

    @Test
    fun `test fetch failure branch`() {
        coEvery { SupabaseClient.fetchAdminEvents(any()) } returns Pair(null, "Connection Failure")

        composeTestRule.setContent {
            Surface {
                AdminEventScreen(AdminEventManager(), "token", {})
            }
        }

        composeTestRule.onNodeWithText("Connection Failure", substring = true).assertExists()
    }
}
