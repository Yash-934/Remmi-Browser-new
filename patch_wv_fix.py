import sys

with open("app/src/main/java/com/remmi/browser/security/WipeVerifier.kt", "r") as f:
    content = f.read()

# Replace the inner vault verification else block
old_block = """        } else {
          if (vaultWiped) {
             vaultVerified = false
             details.add("Cryptographic Audit FAILED: Database file still exists after vault wipe.")
          } else {
             val db = NetRunnerDatabase.getDatabase(context)
             val vaultEntryCount = db.passwordEntryDao().getCount()
             val metaExists = db.masterKeyMetadataDao().getMetadata() != null
             remainingRows += vaultEntryCount

             if (vaultEntryCount == 0 && !metaExists) {
               vaultVerified = true
               details.add("Cryptographic Audit: Verified zeroized password vault entries and metadata.")
             } else {
               vaultVerified = false
               details.add("Cryptographic Audit FAILED: vaultEntryCount=$vaultEntryCount, metaExists=$metaExists")
             }
          }
        }"""

new_block = """        } else {
          vaultVerified = false
          details.add("Cryptographic Audit FAILED: Database file still exists after vault wipe.")
        }"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/remmi/browser/security/WipeVerifier.kt", "w") as f:
    f.write(content)
