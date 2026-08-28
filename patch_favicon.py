import re

with open('app/src/main/java/com/remmi/browser/ui/components/NewTabPage.kt', 'r') as f:
    content = f.read()

old_func = """fun getFaviconUrl(url: String): String {
  // Zero external clearnet leaks for visited domains
  return ""
}"""

new_func = """fun getFaviconUrl(url: String): String {
  return try {
    val uri = android.net.Uri.parse(url)
    val host = uri.host ?: return ""
    "https://www.google.com/s2/favicons?domain=${host}&sz=128"
  } catch (e: Exception) {
    ""
  }
}"""

if old_func in content:
    content = content.replace(old_func, new_func)
    with open('app/src/main/java/com/remmi/browser/ui/components/NewTabPage.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Function not found")
