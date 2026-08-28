import re

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "r") as f:
    content = f.read()

content = content.replace("NetRunnerDatabase.isWipeActive = false", "NetRunnerDatabase.endWipeAfterSuccess()")
content = content.replace("NetRunnerDatabase.isWipeActive = true", "NetRunnerDatabase.beginWipe()")

with open("app/src/test/java/com/remmi/browser/security/PanicWipeManagerTest.kt", "w") as f:
    f.write(content)
