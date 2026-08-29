--- app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
@@ -26,11 +26,7 @@
         is Int -> setList.add(SetGeckoPreference.setIntPref(name, value, branch))
         is Boolean -> setList.add(SetGeckoPreference.setBoolPref(name, value, branch))
-        // double/float/long mappings are not strictly supported by the Gecko pref API (usually only Int/Boolean/String).
-        // Let's fallback to int for numbers, string for others.
-        is Double -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
-        is Float -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
-        is Long -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
-        else -> setList.add(SetGeckoPreference.setStringPref(name, value.toString(), branch))
+        else -> {
+          Log.e(TAG, "Unsupported preference type for key=$name: ${value::class.java.simpleName}")
+          if (cont.isActive) cont.resume(false)
+          return@suspendCancellableCoroutine
+        }
       }
     }
