import re

with open("app/src/main/java/com/remmi/browser/security/PasswordManagerRepository.kt", "r") as f:
    content = f.read()

# Replace constructor property
content = content.replace("private var database: NetRunnerDatabase,", "")
content = content.replace("private var database: NetRunnerDatabase", "")

# Replace the db getter
old_db = """  private val db: NetRunnerDatabase
    get() = if (database.isOpen) database else NetRunnerDatabase.getDatabase(context).also { database = it }"""

new_db = """  private val db: NetRunnerDatabase
    get() = NetRunnerDatabase.getDatabase(context)"""

content = content.replace(old_db, new_db)

# Replace getInstance to only pass context
content = content.replace("PasswordManagerRepository(context.applicationContext, db)", "PasswordManagerRepository(context.applicationContext)")

# Replace the wipe method reference to use db
content = content.replace("database.masterKeyMetadataDao()", "db.masterKeyMetadataDao()")

with open("app/src/main/java/com/remmi/browser/security/PasswordManagerRepository.kt", "w") as f:
    f.write(content)
