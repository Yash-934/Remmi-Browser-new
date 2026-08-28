import sys

with open("app/src/main/java/com/remmi/browser/security/WipeVerifier.kt", "r") as f:
    content = f.read()

# Remove the database parameter
content = content.replace(
"""  suspend fun performLogicalDestructionVerification(
    context: Context,
    database: NetRunnerDatabase,
    vaultWiped: Boolean,
  ): LogicalVerificationReport = withContext(Dispatchers.IO) {""",
"""  suspend fun performLogicalDestructionVerification(
    context: Context,
    vaultWiped: Boolean,
  ): LogicalVerificationReport = withContext(Dispatchers.IO) {""")

# Replace usages of `database` with `NetRunnerDatabase.getDatabase(context)`
# Note: we only hit these if isDbFileAbsent == false.
# But wait, if isDbFileAbsent == false and vaultWiped == true, that means the wipe failed. We shouldn't even try to open it if vaultWiped == true, because it might be corrupted or we don't want to create it.
# Actually, if the file exists and vaultWiped == true, it's a failure anyway. Let's just say remainingRows += 1 to fail it.

new_else_db = """      } else {
        // DB file exists
        if (vaultWiped) {
           dbVerified = false
           remainingRows += 1
           details.add("Database Record Audit FAILED: Database file still exists after full wipe.")
        } else {
           // read row counts
           val db = NetRunnerDatabase.getDatabase(context)
           val historyCount = db.historyDao().getCount()
           val tabCount = db.sessionTabDao().getCount()
           val downloadCount = db.downloadDao().getCount()
           val eventCount = db.blockedEventDao().getCount()

           val totalDbRows = historyCount + tabCount + downloadCount + eventCount
           remainingRows += totalDbRows

           if (totalDbRows == 0) {
             dbVerified = true
             details.add("Database Record Audit: Verified 0 remaining history, tab, download, and event rows.")
           } else {
             dbVerified = false
             details.add("Database Record Audit FAILED: Verified $totalDbRows remaining rows.")
           }
        }
      }"""

# Find the DB read section
start_db = content.find("      } else {\n        // DB file exists - read row counts without mutating")
if start_db != -1:
    end_db = content.find("      }\n    } catch (e: Exception) {", start_db)
    content = content[:start_db] + new_else_db + content[end_db:]

# Now for vault
new_else_vault = """        } else {
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

start_vault = content.find("        } else {\n          // DB file exists - read row count without mutating")
if start_vault != -1:
    end_vault = content.find("        }\n      } catch (e: Exception) {", start_vault)
    content = content[:start_vault] + new_else_vault + content[end_vault:]

with open("app/src/main/java/com/remmi/browser/security/WipeVerifier.kt", "w") as f:
    f.write(content)

