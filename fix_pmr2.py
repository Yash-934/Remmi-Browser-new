import re

with open("app/src/main/java/com/remmi/browser/security/PasswordManagerRepository.kt", "r") as f:
    content = f.read()

content = content.replace("database.", "db.")

with open("app/src/main/java/com/remmi/browser/security/PasswordManagerRepository.kt", "w") as f:
    f.write(content)
