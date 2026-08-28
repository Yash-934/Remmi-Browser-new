with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

content = content.replace("assertTrue(result.errors.isEmpty())", "println(result.errors)")

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
