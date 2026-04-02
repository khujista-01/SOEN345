package com.soen345.ticketreservation.ui.admin

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.soen345.ticketreservation.admin.AdminConstants
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AdminGateScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test correct admin code calls onSuccess`() {
        var successCalled = false
        composeTestRule.setContent {
            AdminGateScreen(
                onSuccess = { successCalled = true },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Admin Code").performTextInput(AdminConstants.ADMIN_CODE)
        composeTestRule.onNodeWithText("Submit").performClick()

        assertTrue(successCalled)
    }

    @Test
    fun `test incorrect admin code shows error`() {
        var successCalled = false
        composeTestRule.setContent {
            AdminGateScreen(
                onSuccess = { successCalled = true },
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Admin Code").performTextInput("WRONG_CODE")
        composeTestRule.onNodeWithText("Submit").performClick()

        composeTestRule.onNodeWithText("Wrong admin code").assertExists()
        assertTrue(!successCalled)
    }

    @Test
    fun `test back button calls onBack`() {
        var backCalled = false
        composeTestRule.setContent {
            AdminGateScreen(
                onSuccess = {},
                onBack = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(backCalled)
    }
}
