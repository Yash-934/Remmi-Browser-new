import re
path = "app/src/main/java/com/remmi/browser/storage/RemmiDatabase.kt"
with open(path, "r") as f:
    content = f.read()

target = """package com.remmi.browser.storage

class VaultRecoveryRequiredException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
"""
replacement = """package com.remmi.browser.storage
"""
content = content.replace(target, replacement)

content += """
class VaultRecoveryRequiredException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
"""

with open(path, "w") as f:
    f.write(content)
