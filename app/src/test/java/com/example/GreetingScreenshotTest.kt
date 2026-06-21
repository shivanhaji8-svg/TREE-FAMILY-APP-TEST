package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.FamilyMember
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val dummy = FamilyMember(
        id = 1,
        name = "Haji Ahmad",
        parentId = null,
        birthYear = "1920",
        note = "The ancestral patriarch of the lineage.",
        generation = 0
    )

    composeTestRule.setContent { 
        MyApplicationTheme { 
            SidebarPropertiesPanel(
                member = dummy,
                onClose = {},
                onUpdate = { _, _, _, _, _ -> },
                onAddRelative = {},
                onDelete = {}
            )
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
