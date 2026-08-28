import re

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "r") as f:
    content = f.read()

content = content.replace(
    "@Volatile\n    var isWipeActive: Boolean = false",
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
    }'''
)

with open("app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt", "w") as f:
    f.write(content)
