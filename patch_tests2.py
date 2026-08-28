import sys

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

# Strip out test34 to the end
idx = content.find("fun test34_DbIsNeverReopenedDuringWipe")
if idx != -1:
    # Find the @Test before it
    idx = content.rfind("@Test", 0, idx)
    content = content[:idx]

new_tests = """  @Test
  fun test34_DbIsNeverReopenedDuringWipe() {
    val ctx = context
    kotlinx.coroutines.runBlocking {
      NetRunnerDatabase.isWipeActive = true
      try {
          NetRunnerDatabase.getDatabase(ctx)
          org.junit.Assert.fail("Database should not reopen during wipe")
      } catch (e: IllegalStateException) {
          assertTrue(e.message!!.contains("Cannot open database during an active Panic Wipe"))
      } finally {
          NetRunnerDatabase.isWipeActive = false
      }
    }
  }

  @Test
  fun test35_WipeVaultFalse_PreservesDbAndKeys() {
    val ctx = context
    kotlinx.coroutines.runBlocking {
      val dbFile = ctx.getDatabasePath("netrunner_vault.db")
      dbFile.parentFile?.mkdirs()
      dbFile.writeText("fake db")
      
      val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      try {
        val keyGen = javax.crypto.KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(android.security.keystore.KeyGenParameterSpec.Builder("netrunner_db_master_key", android.security.keystore.KeyProperties.PURPOSE_ENCRYPT).build())
        keyGen.generateKey()
      } catch (e: Exception) {}

      PanicWipeManager.executeWipe(ctx, NetRunnerDatabase.getDatabase(ctx), wipeVault = false)
      
      assertTrue("DB file should be preserved if wipeVault=false", dbFile.exists())
      assertTrue("Keystore should be preserved if wipeVault=false", ks.containsAlias("netrunner_db_master_key"))
      
      assertFalse(NetRunnerDatabase.isWipeActive)
    }
  }
  
  @Test
  fun test36_VaultScrubBeforeDbClose() {
     val ctx = context
     kotlinx.coroutines.runBlocking {
       val result = NetRunnerDatabase.secureWipe(ctx, true) {
           true
       }
       assertTrue(result.vaultScrubSucceeded)
     }
  }

  @Test
  fun test37_DbAndWalAndShmAbsentAfterFullWipe() {
     val ctx = context
     kotlinx.coroutines.runBlocking {
       val dbFile = ctx.getDatabasePath("netrunner_vault.db")
       dbFile.parentFile?.mkdirs()
       dbFile.writeText("fake db")
       java.io.File(dbFile.path + "-wal").writeText("wal")
       java.io.File(dbFile.path + "-shm").writeText("shm")
       java.io.File(dbFile.path + "-journal").writeText("journal")
       
       NetRunnerDatabase.secureWipe(ctx, true) { true }
       
       assertFalse(dbFile.exists())
       assertFalse(java.io.File(dbFile.path + "-wal").exists())
       assertFalse(java.io.File(dbFile.path + "-shm").exists())
       assertFalse(java.io.File(dbFile.path + "-journal").exists())
     }
  }

  @Test
  fun test38_DeeplyNestedGeckoUserData() {
     val ctx = context
     kotlinx.coroutines.runBlocking {
       val cacheDir = java.io.File(ctx.cacheDir, "gecko/deeply/nested/dir/1/2/3/4/5/6/7/8/9/10/cookies.sqlite")
       cacheDir.parentFile?.mkdirs()
       cacheDir.writeText("data")
       
       val residuals = mutableListOf<String>()
       val unique = mutableSetOf<String>()
       WipeVerifier.scanGeckoSensitiveArtifacts(ctx, residuals, unique)
       
       assertTrue(residuals.isNotEmpty())
       assertTrue(residuals[0].contains("cookies.sqlite"))
     }
  }

  @Test
  fun test39_CryptoArtifactPolicy() {
     kotlinx.coroutines.runBlocking {
       val file1 = java.io.File("cert9.db")
       val file2 = java.io.File("key4.db")
       assertEquals(WipeVerifier.GeckoArtifactClassification.UNKNOWN_GECKO_CRYPTO_ARTIFACT, WipeVerifier.classifyGeckoArtifact(file1))
       assertEquals(WipeVerifier.GeckoArtifactClassification.UNKNOWN_GECKO_CRYPTO_ARTIFACT, WipeVerifier.classifyGeckoArtifact(file2))
     }
  }
}
"""

content += new_tests

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
