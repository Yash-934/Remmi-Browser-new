import re

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "r") as f:
    content = f.read()

# 1. Update companion object with lock and synchronized transitions
old_companion = """  companion object {
    enum class WipeState {
      IDLE,
      ACTIVE,
      RECOVERY_REQUIRED
    }

    @Volatile private var wipeState: WipeState = WipeState.IDLE

    val isWipeActive: Boolean
      get() = wipeState != WipeState.IDLE

    internal fun beginWipe() {
      wipeState = WipeState.ACTIVE
    }

    internal fun endWipeAfterSuccess() {
      wipeState = WipeState.IDLE
    }
    
    internal fun endWipeWithFailure() {
      wipeState = WipeState.RECOVERY_REQUIRED
    }"""

new_companion = """  companion object {
    enum class WipeState {
      IDLE,
      ACTIVE,
      RECOVERY_REQUIRED
    }

    private val dbLock = java.util.concurrent.locks.ReentrantReadWriteLock(true)

    @Volatile private var wipeState: WipeState = WipeState.IDLE

    val isWipeActive: Boolean
      get() = wipeState != WipeState.IDLE

    internal fun beginWipe() {
      synchronized(this) {
        if (wipeState == WipeState.ACTIVE) {
          throw IllegalStateException("Wipe already in progress.")
        }
        wipeState = WipeState.ACTIVE
      }
    }

    internal fun endWipeAfterSuccess() {
      synchronized(this) {
        wipeState = WipeState.IDLE
      }
    }
    
    internal fun endWipeWithFailure() {
      synchronized(this) {
        wipeState = WipeState.RECOVERY_REQUIRED
      }
    }

    fun <T> withDatabase(context: android.content.Context, block: (NetRunnerDatabase) -> T): T {
        dbLock.readLock().lock()
        try {
            return block(getDatabase(context))
        } finally {
            dbLock.readLock().unlock()
        }
    }"""
content = content.replace(old_companion, new_companion)

# 2. Refactor secureWipe to use try/catch/finally and writeLock
old_secure_wipe_start = """    suspend fun secureWipe(
      context: Context,
      wipeVault: Boolean = false,
      vaultScrubber: suspend () -> Boolean = { true }
    ): PurgeResult {
      beginWipe()
      var vaultScrubbed = false
      var browserTablesScrubbed = true
      val errors = mutableListOf<String>()"""

new_secure_wipe_start = """    suspend fun secureWipe(
      context: Context,
      wipeVault: Boolean = false,
      vaultScrubber: suspend () -> Boolean = { true }
    ): PurgeResult {
      beginWipe()
      var vaultScrubbed = false
      var browserTablesScrubbed = true
      val errors = mutableListOf<String>()
      
      var isSuccess = false
      dbLock.writeLock().lock()
      try {"""

content = content.replace(old_secure_wipe_start, new_secure_wipe_start)

# Now find the end of secureWipe to close the try block
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

