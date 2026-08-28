
  @Test
  fun test40_FreshVaultAfterWipe() = kotlinx.coroutines.runBlocking {
      val ctx = context
      NetRunnerDatabase.endWipeAfterSuccess()
      
      // 1. Create DB and vault
      val db1 = NetRunnerDatabase.getDatabase(ctx)
      db1.sessionTabDao().insertTab(com.remmi.browser.storage.SessionTabEntity(url = "https://example.com", title = "Test", timestamp = 1L))
      
      // 2. Panic wipe with wipeVault=true
      val result = NetRunnerDatabase.secureWipe(ctx, true) { true }
      assertTrue(result.errors.isEmpty())
      
      // 3. Verification
      assertFalse("isWipeActive should be false after successful wipe", NetRunnerDatabase.isWipeActive)
      
      // 4. Initialize fresh DB
      val db2 = NetRunnerDatabase.getDatabase(ctx)
      
      // 5. Fresh vault initialization works and old data does not return
      val tabs = db2.sessionTabDao().getAllTabs()
      assertTrue("Old vault data should not return", tabs.isEmpty())
  }
  
  @Test
  fun test41_ConcurrentGetDatabaseVsSecureWipeRace() {
      val ctx = context
      NetRunnerDatabase.endWipeAfterSuccess()
      
      var errors = 0
      
      for (i in 0..100) {
          NetRunnerDatabase.endWipeAfterSuccess()
          
          val threadA = Thread {
              try {
                  NetRunnerDatabase.getDatabase(ctx)
              } catch (e: IllegalStateException) {
                  // Expected if wipe is active
              }
          }
          
          val threadB = Thread {
              kotlinx.coroutines.runBlocking {
                  NetRunnerDatabase.secureWipe(ctx, true) { true }
              }
          }
          
          threadA.start()
          threadB.start()
          
          threadA.join()
          threadB.join()
      }
      
      NetRunnerDatabase.endWipeAfterSuccess()
  }
