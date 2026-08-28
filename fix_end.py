import re

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "r") as f:
    content = f.read()

old_secure_wipe_end = """      // Evaluate wipe success
      val isSuccess = errors.isEmpty() && (!wipeVault || vaultScrubbed)
      
      if (isSuccess) {
          endWipeAfterSuccess()
      } else {
          endWipeWithFailure()
      }
      return PurgeResult(
        filesDeleted = deleted,
        filesFailed = failed,
        keyRevoked = keyRevoked,
        errors = errors,
        vaultScrubSucceeded = vaultScrubbed
      )
    }"""

new_secure_wipe_end = """      // Evaluate wipe success
      isSuccess = errors.isEmpty() && (!wipeVault || vaultScrubbed)
      return PurgeResult(
        filesDeleted = deleted,
        filesFailed = failed,
        keyRevoked = keyRevoked,
        errors = errors,
        vaultScrubSucceeded = vaultScrubbed
      )
      } catch (e: Exception) {
          errors.add("Unexpected exception during secureWipe: ${e.message}")
          return PurgeResult(0, 0, false, false, errors)
      } finally {
          dbLock.writeLock().unlock()
          if (isSuccess) {
              endWipeAfterSuccess()
          } else {
              endWipeWithFailure()
          }
      }
    }"""

content = content.replace(old_secure_wipe_end, new_secure_wipe_end)
with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "w") as f:
    f.write(content)

