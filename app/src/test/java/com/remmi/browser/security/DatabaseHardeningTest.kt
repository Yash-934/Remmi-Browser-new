package com.remmi.browser.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.remmi.browser.storage.RemmiDatabase
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseHardeningTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    RemmiDatabase.closeDatabase()
  }

  @After
  fun tearDown() {
    RemmiDatabase.testPassphraseProvider = null
    RemmiDatabase.closeDatabase()
  }

  @Test
  fun testFailsClosedWhenKeyDerivationFails() {
    RemmiDatabase.testPassphraseProvider = {
      throw SecurityException("Hardware Keystore failure simulation")
    }

    assertThrows(SecurityException::class.java) {
      RemmiDatabase.getDatabase(context)
    }
  }

  @Test
  fun testFailsClosedWhenKeystoreUnavailableWithoutTestProvider() {
    // When no test provider is set and running in non-hardware Keystore env, must throw SecurityException
    RemmiDatabase.testPassphraseProvider = null
    assertThrows(SecurityException::class.java) {
      RemmiDatabase.getDatabase(context)
    }
  }
}
