# Fix the end of SettingsRepository.kt
# We'll just take the file up to `resetSpeedDials` and then manually append what's needed.
sed -i '/fun resetSpeedDials() {/,+10d' app/src/main/java/com/remmi/browser/storage/SettingsRepository.kt

cat << 'INNER_EOF' >> app/src/main/java/com/remmi/browser/storage/SettingsRepository.kt
  fun resetSpeedDials() {
    saveSpeedDials(DEFAULT_SPEED_DIALS)
  }

  fun updateRestoreLastSession(enabled: Boolean) {
    val editor = prefs.edit()
    editor.putBoolean("restore_last_session", enabled)
    if (enabled) {
      editor.putBoolean("clear_on_exit", false)
      _settings.value = _settings.value.copy(
        restoreLastSession = true,
        clearDataOnExit = false
      )
    } else {
      _settings.value = _settings.value.copy(restoreLastSession = false)
    }
    editor.apply()
  }

  fun updateClearOnExit(enabled: Boolean) {
    val editor = prefs.edit()
    editor.putBoolean("clear_on_exit", enabled)
    _settings.value = _settings.value.copy(
      clearDataOnExit = enabled
    )
    editor.apply()
  }

  companion object {
    @Volatile
    private var INSTANCE: SettingsRepository? = null
    fun getInstance(context: Context): SettingsRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
INNER_EOF
