--- app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
+++ app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
@@ -140,8 +140,11 @@
   }
 
   /**
    * Launches an external translator app/browser for the given text.
    */
-  fun launchExternalTranslator(context: Context, urlOrText: String) {
+  fun launchExternalTranslator(context: Context, urlOrText: String, isGhost: Boolean = false) {
+    if (isGhost) {
+      Log.w(TAG, "External intent blocked in Ghost mode to prevent IP leak via ACTION_VIEW")
+      return
+    }
     try {
       val targetUri = if (urlOrText.startsWith("http://") || urlOrText.startsWith("https://")) {
