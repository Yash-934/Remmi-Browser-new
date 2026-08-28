import re

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "r") as f:
    content = f.read()

# 1. Replace isWipeActive definition with wipeState
content = re.sub(
    r'@Volatile var isWipeActive: Boolean = false',
    '''enum class WipeState {
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
    }''',
    content
)

# 2. Fix getDatabase
get_database_old = '''    fun getDatabase(context: Context): NetRunnerDatabase {
      if (isWipeActive) throw IllegalStateException("Cannot open database during an active Panic Wipe.")
      return INSTANCE ?: synchronized(this) {
        val passphrase = getOrCreatePassphrase(context)
        try {
          val factory = SupportFactory(passphrase)
          val instance = Room.databaseBuilder(
            context.applicationContext,
            NetRunnerDatabase::class.java,
            "netrunner_vault.db"
          )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
          INSTANCE = instance
          instance
        } finally {
          passphrase.fill(0) // Immediate in-place memory zeroization of temporary passphrase
        }
      }
    }'''

get_database_new = '''    fun getDatabase(context: Context): NetRunnerDatabase {
      return synchronized(this) {
        if (wipeState != WipeState.IDLE) {
          throw IllegalStateException("Cannot open database during an active Panic Wipe.")
        }
        var instance = INSTANCE
        if (instance != null) {
          return@synchronized instance
        }
        val passphrase = getOrCreatePassphrase(context)
        try {
          val factory = SupportFactory(passphrase)
          instance = Room.databaseBuilder(
            context.applicationContext,
            NetRunnerDatabase::class.java,
            "netrunner_vault.db"
          )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
          INSTANCE = instance
          instance
        } finally {
          passphrase.fill(0) // Immediate in-place memory zeroization of temporary passphrase
        }
      }
    }'''

content = content.replace(get_database_old, get_database_new)

# 3. Fix secureWipe usages of isWipeActive
secure_wipe_old = '''      isWipeActive = true
      var vaultScrubbed = false'''
secure_wipe_new = '''      beginWipe()
      var vaultScrubbed = false'''

content = content.replace(secure_wipe_old, secure_wipe_new)

wipe_active_false_old = '''      if (!wipeVault) {
          // If we aren't wiping the vault, we can unset the wipe active flag
          // so the app continues normally after the panic wipe (which cleared browser data).
          isWipeActive = false
      }'''

wipe_active_false_new = '''      // Evaluate wipe success
      val isSuccess = errors.isEmpty() && (!wipeVault || vaultScrubbed)
      
      if (isSuccess) {
          endWipeAfterSuccess()
      } else {
          endWipeWithFailure()
      }'''

content = content.replace(wipe_active_false_old, wipe_active_false_new)

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "w") as f:
    f.write(content)
