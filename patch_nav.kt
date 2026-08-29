--- app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
@@ -372,6 +372,20 @@
     newSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
+      override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
+        val url = request.uri
+        val tab = TabManager.getInstance().getTab(tabId)
+        val isGhost = (tab?.profile == PrivacyProfile.GHOST) || (currentProfile == PrivacyProfile.GHOST)
+        
+        if (isGhost) {
+          val isHttp = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:") || url.startsWith("file:") || url.startsWith("blob:")
+          if (!isHttp) {
+            Log.w(TAG, "Blocked non-HTTP intent/scheme in Ghost Mode: $url")
+            return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
+          }
+        }
+        return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.ALLOW)
+      }
+
       override fun onLocationChange(
