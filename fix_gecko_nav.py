import re
path = "app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt"
with open(path, "r") as f:
    content = f.read()

target = """      override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
        val url = request.uri
        val tab = TabManager.getInstance().getTab(tabId)
        val isGhost = (tab?.profile == PrivacyProfile.GHOST) || (currentProfile == PrivacyProfile.GHOST)
        
        if (isGhost) {
          val isHttp = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("file:") || url.startsWith("blob:") || url.startsWith("data:")
          if (!isHttp) {
            Log.w(TAG, "Blocked non-HTTP intent/scheme in Ghost Mode: $url")
            return GeckoResult.fromValue(AllowOrDeny.DENY)
          }
        }
        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
      }"""

replacement = """      override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
        val url = request.uri
        val tab = TabManager.getInstance().getTab(tabId)
        val isGhost = (tab?.profile == PrivacyProfile.GHOST) || (currentProfile == PrivacyProfile.GHOST)
        
        // Ensure .onion domains are strictly rejected in clearnet
        val isOnion = com.remmi.browser.security.NetworkRouteAuthority.isOnionDestination(url)
        if (isOnion && !isGhost) {
            Log.e(TAG, "Blocked .onion request in clearnet: $url")
            return GeckoResult.fromValue(AllowOrDeny.DENY)
        }
        
        if (isGhost) {
          val isHttp = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("file:") || url.startsWith("blob:") || url.startsWith("data:")
          if (!isHttp) {
            Log.w(TAG, "Blocked non-HTTP intent/scheme in Ghost Mode: $url")
            return GeckoResult.fromValue(AllowOrDeny.DENY)
          }
        }
        return GeckoResult.fromValue(AllowOrDeny.ALLOW)
      }"""

content = content.replace(target, replacement)
with open(path, "w") as f:
    f.write(content)
