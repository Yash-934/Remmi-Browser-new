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
    
    // Do NOT forcefully change restoreLastSession here in the database or state.
    // If Clear Data is on, the UI will ignore restoreLastSession and show it as disabled.
    // But the actual preference is kept so that if Clear Data is turned off,
    // Restore Previous Session returns to its previous valid state.
    _settings.value = _settings.value.copy(
      clearDataOnExit = enabled
    )
    editor.apply()
  }
