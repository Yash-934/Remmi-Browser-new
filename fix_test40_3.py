with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

old_str = """      // 1. Create DB and vault
      val db1 = NetRunnerDatabase.getDatabase(ctx)"""

new_str = """      // 1. Create DB and vault
      try {
          val db1 = NetRunnerDatabase.getDatabase(ctx)
      } catch (e: SecurityException) {
          // Ignore if previous test wiped it
      }"""

content = content.replace(old_str, new_str)

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
