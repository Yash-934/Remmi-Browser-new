with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

content = content.replace("db1.sessionTabDao().insertTab(com.remmi.browser.storage.SessionTabEntity(url = \"https://example.com\", title = \"Test\", timestamp = 1L))", "")

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
