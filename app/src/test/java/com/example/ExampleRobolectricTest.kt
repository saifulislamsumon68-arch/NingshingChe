package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.NinghsingCheContentData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("নিংশিং চে", appName)
  }

  @Test
  fun `verify content data loaded`() {
    assertNotNull(NinghsingCheContentData.categories)
    assertNotNull(NinghsingCheContentData.pdfDocuments)
    assertEquals(true, NinghsingCheContentData.categories.isNotEmpty())
  }

  @Test
  fun `launch MainActivity test`() {
    org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
  }
}

