import re
path = "app/src/main/java/com/remmi/browser/downloads/DownloadHandler.kt"
with open(path, "r") as f:
    content = f.read()

target = """        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
          throw java.io.IOException("Unexpected HTTP code: ${response.code}")
        }
"""
replacement = """        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
          throw java.io.IOException("Unexpected HTTP code: ${response.code}")
        }
        
        val contentLength = response.body?.contentLength() ?: -1L
        if (contentLength > MAX_DOWNLOAD_SIZE_BYTES) {
          response.close()
          throw java.io.IOException("Download preflight rejected: Content-Length ($contentLength bytes) exceeds 5GB limit.")
        }
"""
content = content.replace(target, replacement)
with open(path, "w") as f:
    f.write(content)
