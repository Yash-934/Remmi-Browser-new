import re
path = "app/src/main/java/com/remmi/browser/downloads/DownloadHandler.kt"
with open(path, "r") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.cancelAndJoin\npackage com.remmi.browser.downloads", "package com.remmi.browser.downloads\nimport kotlinx.coroutines.cancelAndJoin")
with open(path, "w") as f:
    f.write(content)
