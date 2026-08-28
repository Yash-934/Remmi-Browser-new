import sys

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "r") as f:
    content = f.read()

# We need to refine secureWipe to only delete files if wipeVault=true.
# Wait, actually, does the user want the database deleted if wipeVault=true? Yes.
# What if wipeVault=false? Then we only clear browser tables and checkpoint, but we DO NOT delete the file, 
# and we DO NOT close the database forever (well we can close it and let it reopen, or we can just keep it open).

old_func_start = """    suspend fun secureWipe("""
old_func_end = """      return PurgeResult(
        filesDeleted = deleted,
        filesFailed = failed,
        keyRevoked = keyRevoked,
        errors = errors,
        vaultScrubSucceeded = vaultScrubbed
      )
    }"""

# Re-read file and do a surgical replace
start_idx = content.find("suspend fun secureWipe(")
end_idx = content.find("return PurgeResult(", start_idx)
end_idx = content.find("}", end_idx) + 1

new_func = """    suspend fun secureWipe(
      context: Context,
      wipeVault: Boolean = false,
      vaultScrubber: suspend () -> Boolean = { true }
    ): PurgeResult {
      isWipeActive = true
      var vaultScrubbed = false
      var browserTablesScrubbed = true
      val errors = mutableListOf<String>()

      // 1. Always scrub browser data
      INSTANCE?.let { db ->
        if (db.isOpen) {
          try {
            db.historyDao().clearHistory()
            db.sessionTabDao().clearAllTabs()
            db.downloadDao().clearAll()
            db.blockedEventDao().clearAll()
          } catch (e: Exception) {
            browserTablesScrubbed = false
            errors.add("Browser tables scrub failed: ${e.message}")
          }
        }
      }
      
      // 2. Vault Scrub if requested
      if (wipeVault) {
        try {
          vaultScrubbed = vaultScrubber()
        } catch (e: Exception) {
          errors.add("Vault scrub failed: ${e.message}")
        }
      }
      
      // 3. DB Checkpoint and Close
      closeDatabase()
      
      var deleted = 0
      var failed = 0
      var keyRevoked = false

      // 4. Purge Database Files only if vault is wiped 
      // (since vault is the only thing sharing this DB, if vault is wiped, the whole DB is wiped)
      if (wipeVault) {
        val dbFile = context.getDatabasePath("netrunner_vault.db")
        val dbDir = dbFile.parentFile
        if (dbFile.exists()) {
          try {
            if (dbFile.delete()) deleted++ else {
              failed++
              errors.add("Failed to delete database file: ${dbFile.name}")
            }
          } catch (e: Exception) {
            failed++
            errors.add("Exception deleting database file: ${e.message}")
          }
        }

        if (dbDir != null && dbDir.exists()) {
          dbDir.listFiles()?.forEach { file ->
            val n = file.name.lowercase()
            if (n.startsWith("netrunner_vault") || n.startsWith("netrunner_browser")) {
              try {
                if (file.delete()) deleted++ else {
                  failed++
                  errors.add("Failed to delete database journal/artifact: ${file.name}")
                }
              } catch (e: Exception) {
                failed++
                errors.add("Exception deleting journal file ${file.name}: ${e.message}")
              }
            }
          }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
          val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
          if (ks.containsAlias(KEY_ALIAS)) {
            ks.deleteEntry(KEY_ALIAS)
          }
          keyRevoked = !ks.containsAlias(KEY_ALIAS)
          if (!keyRevoked) {
            errors.add("Keystore alias $KEY_ALIAS was still present after deletion.")
          }
        } catch (e: Exception) {
          errors.add("Failed to revoke database master keystore key: ${e.message}")
        }
      }

      if (!wipeVault) {
          // If we aren't wiping the vault, we can unset the wipe active flag
          // so the app continues normally after the panic wipe (which cleared browser data).
          isWipeActive = false
      }

      return PurgeResult(
        filesDeleted = deleted,
        filesFailed = failed,
        keyRevoked = keyRevoked,
        errors = errors,
        vaultScrubSucceeded = vaultScrubbed
      )
    }"""

content = content[:start_idx] + new_func + content[end_idx:]

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "w") as f:
    f.write(content)

