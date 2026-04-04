package com.soen345.ticketreservation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===================== ORIGINAL TESTS =====================

    @Config(sdk = [33])
    @Test
    fun `test theme with dynamic colors modern SDK`() {
        composeTestRule.setContent {
            TicketReservationTheme(dynamicColor = true) {
                Text("Theme Test")
            }
        }
        composeTestRule.onNodeWithText("Theme Test").assertExists()
    }

    @Config(sdk = [33])
    @Test
    fun `test light theme manual`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = false, dynamicColor = false) {
                assertNotNull(MaterialTheme.colorScheme.primary)
                Text("Light Mode")
            }
        }
        composeTestRule.onNodeWithText("Light Mode").assertExists()
    }

    @Config(sdk = [33])
    @Test
    fun `test dark theme manual`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = true, dynamicColor = false) {
                assertNotNull(MaterialTheme.colorScheme.primary)
                Text("Dark Mode")
            }
        }
        composeTestRule.onNodeWithText("Dark Mode").assertExists()
    }

    @Config(sdk = [33])
    @Test
    fun `test colors and typography coverage`() {
        assertNotNull(Purple80)
        assertNotNull(PurpleGrey80)
        assertNotNull(Pink80)
        assertNotNull(Purple40)
        assertNotNull(PurpleGrey40)
        assertNotNull(Pink40)
        assertNotNull(Typography)
    }

    // ===================== BRANCH COVERAGE TESTS =====================

    // Branch 1: dynamicColor=true AND SDK >= S (31) → dynamic dark scheme
    @Config(sdk = [31])
    @Test
    fun `dynamic dark theme on Android S`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = true, dynamicColor = true) {
                Text("DynamicDark")
            }
        }
        composeTestRule.onNodeWithText("DynamicDark").assertExists()
    }

    // Branch 1b: dynamicColor=true AND SDK >= S → dynamic light scheme
    @Config(sdk = [31])
    @Test
    fun `dynamic light theme on Android S`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = false, dynamicColor = true) {
                Text("DynamicLight")
            }
        }
        composeTestRule.onNodeWithText("DynamicLight").assertExists()
    }

    // Branch 2: SDK < S → falls through to static dark scheme
    @Config(sdk = [30])
    @Test
    fun `static dark theme below Android S`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = true, dynamicColor = true) {
                Text("StaticDark")
            }
        }
        composeTestRule.onNodeWithText("StaticDark").assertExists()
    }

    // Branch 3: SDK < S → falls through to static light scheme (else)
    @Config(sdk = [30])
    @Test
    fun `static light theme below Android S`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = false, dynamicColor = true) {
                Text("StaticLight")
            }
        }
        composeTestRule.onNodeWithText("StaticLight").assertExists()
    }

    // Explicit else branch: dynamicColor=false, darkTheme=false
    @Config(sdk = [33])
    @Test
    fun `no dynamic no dark hits else branch`() {
        composeTestRule.setContent {
            TicketReservationTheme(darkTheme = false, dynamicColor = false) {
                Text("ElseBranch")
            }
        }
        composeTestRule.onNodeWithText("ElseBranch").assertExists()
    }
}
