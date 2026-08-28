import sys

with open("app/src/main/java/com/remmi/browser/security/PanicWipeManager.kt", "r") as f:
    content = f.read()

# We need to restructure the phase orchestration.
# Currently, Step 3 is WIPE_PASSWORD_VAULT and Step 4 is SCRUB_DATABASE.
# The user says:
# "PanicWipeManager should orchestrate phases, not duplicate DB lifecycle logic."

# We'll merge Step 3 and 4 visually into the secureWipe call, but report them separately in telemetry.

# Find the start of Step 3
start_idx = content.find("// Step 3 (Conditional): WIPE_PASSWORD_VAULT")
end_idx = content.find("// Step 5: WIPE_DISK_STORAGE")

if start_idx != -1 and end_idx != -1:
    old_steps = content[start_idx:end_idx]
    
    new_steps = """// Step 3 & 4 Orchestrated: WIPE_PASSWORD_VAULT and SCRUB_DATABASE
      _state.value = PanicWipeState.InProgress(
        phaseDescription = "${WipePhase.SCRUB_DATABASE.title}...",
        progress = 4f / totalSteps,
        currentPhase = WipePhase.SCRUB_DATABASE,
        verifiedSteps = telemetryList.toList()
      )

      try {
        val purgeResult = NetRunnerDatabase.secureWipe(context, wipeVault) {
          _state.value = PanicWipeState.InProgress(
            phaseDescription = "${WipePhase.WIPE_PASSWORD_VAULT.title}...",
            progress = 3f / totalSteps,
            currentPhase = WipePhase.WIPE_PASSWORD_VAULT,
            verifiedSteps = telemetryList.toList()
          )
          val pmRepo = PasswordManagerRepository.getInstance(context)
          val success = pmRepo.wipeAllVaultData()
          PasswordManagerRepository.resetInstance()
          success
        }

        if (wipeVault) {
          if (purgeResult.vaultScrubSucceeded) {
            telemetryList.add(WipeStepTelemetry(WipePhase.WIPE_PASSWORD_VAULT, true, "Vault records & cryptographic master keys destroyed"))
          } else {
            telemetryList.add(WipeStepTelemetry(WipePhase.WIPE_PASSWORD_VAULT, false, "Vault wipe reported failure or key revocation failed"))
          }
        }

        if (purgeResult.filesFailed == 0 && purgeResult.errors.isEmpty()) {
          telemetryList.add(WipeStepTelemetry(WipePhase.SCRUB_DATABASE, true, "Database files (${purgeResult.filesDeleted} files) and journals erased"))
        } else {
          val errorMsg = if (purgeResult.errors.isNotEmpty()) purgeResult.errors.joinToString() else "Failed to delete ${purgeResult.filesFailed} database file(s)"
          telemetryList.add(WipeStepTelemetry(WipePhase.SCRUB_DATABASE, false, "Database purge encountered issues: $errorMsg"))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Database secure wipe error: ${e.message}")
        telemetryList.add(WipeStepTelemetry(WipePhase.SCRUB_DATABASE, false, "Database secure wipe failed: ${e.message}"))
      }

      """
    
    content = content[:start_idx] + new_steps + content[end_idx:]
    
    with open("app/src/main/java/com/remmi/browser/security/PanicWipeManager.kt", "w") as f:
        f.write(content)
else:
    print("Could not find step 3 and 5 markers")

