--- app/src/main/java/com/remmi/browser/storage/RemmiDatabase.kt
+++ app/src/main/java/com/remmi/browser/storage/RemmiDatabase.kt
@@ -10,6 +10,8 @@
 import kotlinx.coroutines.flow.MutableStateFlow
 import kotlinx.coroutines.flow.StateFlow
 import kotlinx.coroutines.flow.asStateFlow
+import kotlinx.coroutines.Deferred
+import kotlinx.coroutines.async
 import kotlinx.coroutines.launch
 import kotlinx.coroutines.sync.Mutex
 import kotlinx.coroutines.sync.withLock
@@ -594,7 +596,9 @@
     val databaseState: StateFlow<DatabaseState> = _databaseState.asStateFlow()
 
-    private val initMutex = Mutex()
+    @Volatile
+    private var initDeferred: Deferred<RemmiDatabase>? = null
+    private val initMutex = Mutex() // Only to protect initDeferred assignment
 
     fun prefetchDatabaseAsync(context: Context, scope: CoroutineScope) {
       scope.launch {
@@ -625,31 +629,32 @@
         }
         return@withContext existing
       }
-      initMutex.withLock {
-        var instance = INSTANCE
-        if (instance != null && instance.isOpen) {
-          if (_databaseState.value !is DatabaseState.Ready) {
-            _databaseState.value = DatabaseState.Ready(instance)
-          }
-          return@withLock instance
-        }
-        val startTime = android.os.SystemClock.elapsedRealtime()
-        try {
-          net.sqlcipher.database.SQLiteDatabase.loadLibs(context.applicationContext)
-        } catch (_: Throwable) {}
-        val passphrase = getOrCreatePassphrase(context.applicationContext)
-        val supportFactory = SupportFactory(passphrase, null, false)
-        instance = Room.databaseBuilder(
-          context.applicationContext,
-          RemmiDatabase::class.java,
-          "remmi_vault.db"
-        )
-          .openHelperFactory(supportFactory)
-          .fallbackToDestructiveMigration(false)
-          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
-          .build()
-        INSTANCE = instance
-        _databaseState.value = DatabaseState.Ready(instance)
-        val duration = android.os.SystemClock.elapsedRealtime() - startTime
-        android.util.Log.i("RemmiDatabase", "Asynchronous database initialization completed in ${duration}ms")
-        instance
+      
+      val deferred = initMutex.withLock {
+        var d = initDeferred
+        if (d == null) {
+          val ctx = context.applicationContext
+          d = kotlinx.coroutines.GlobalScope.async(Dispatchers.IO) {
+            val startTime = android.os.SystemClock.elapsedRealtime()
+            try {
+              net.sqlcipher.database.SQLiteDatabase.loadLibs(ctx)
+            } catch (_: Throwable) {}
+            val passphrase = getOrCreatePassphrase(ctx)
+            val supportFactory = SupportFactory(passphrase, null, false)
+            val instance = Room.databaseBuilder(
+              ctx,
+              RemmiDatabase::class.java,
+              "remmi_vault.db"
+            )
+              .openHelperFactory(supportFactory)
+              .fallbackToDestructiveMigration(false)
+              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
+              .build()
+            INSTANCE = instance
+            _databaseState.value = DatabaseState.Ready(instance)
+            val duration = android.os.SystemClock.elapsedRealtime() - startTime
+            android.util.Log.i("RemmiDatabase", "Asynchronous database initialization completed in ${duration}ms")
+            initDeferred = null
+            instance
+          }
+          initDeferred = d
+        }
+        d
       }
+      deferred.await()
     }
