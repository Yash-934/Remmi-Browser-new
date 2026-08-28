package com.remmi.browser

import android.app.Application
import android.util.Log
import com.netrunner.adblock.AdblockBridge
import com.remmi.browser.engine.GeckoEngineManager
import com.remmi.browser.storage.NetRunnerDatabase
import com.remmi.browser.storage.SettingsRepository
import java.io.File
import java.util.concurrent.Executors

class RemmiApp : Application() {

  override fun onCreate() {
    super.onCreate()

    // Global Uncaught Exception Handler to capture crash logs & export to Downloads
    com.remmi.browser.util.CrashHandlerHelper.install(this)

    // Initialize local storage and settings in background to keep startup instant
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
      try {
        Log.i("RemmiApp", "Background initialization started...")
        AdblockBridge.getInstance()
        NetRunnerDatabase.getDatabase(this)
        SettingsRepository.getInstance(this)
        Log.i("RemmiApp", "Background initialization completed.")
      } catch (e: Throwable) {
        Log.e("RemmiApp", "Error during background init: ${e.message}", e)
      }
    }
  }
}

