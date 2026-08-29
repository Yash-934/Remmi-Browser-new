--- app/src/test/java/com/remmi/browser/security/GeckoProxyApplicationRegressionTest.kt
+++ app/src/test/java/com/remmi/browser/security/GeckoProxyApplicationRegressionTest.kt
@@ -4,6 +4,7 @@
 import androidx.test.core.app.ApplicationProvider
 import com.remmi.browser.engine.GeckoEngineManager
 import io.mockk.coEvery
+import io.mockk.every
 import io.mockk.mockk
 import io.mockk.mockkObject
 import io.mockk.unmockkAll
@@ -23,6 +24,8 @@
         CurrentTorRoute.clearRoute()
         mockkObject(TorStatusChecker)
         mockkObject(NetworkHardening)
+        mockkObject(GeckoEngineManager)
+        mockkObject(TorManager)
     }
 
     @After
@@ -32,23 +35,26 @@
 
     @Test
     fun testGhostModeAbortsOnGeckoProxyFailure() = runTest {
-        val geckoEngine = mockk<GeckoEngineManager>(relaxed = true)
-        val torManager = mockk<TorManager>(relaxed = true)
-
-        // Tor daemon succeeds
-        coEvery { torManager.startTor() } returns Result.success(9050)
+        val mockGeckoEngine = mockk<GeckoEngineManager>(relaxed = true)
+        val mockTorManager = mockk<TorManager>(relaxed = true)
+
+        every { GeckoEngineManager.getInstance(any()) } returns mockGeckoEngine
+        every { TorManager.getInstance(any()) } returns mockTorManager
+
+        // Tor daemon succeeds
+        coEvery { mockTorManager.startTor() } returns Result.success(9050)
+        every { mockTorManager.currentCircuit.value } returns null
         
         // Handshake succeeds
         every { TorStatusChecker.isPortListening(any(), any(), any()) } returns true
         every { TorStatusChecker.verifySocks5Handshake(any(), any(), any()) } returns true
 
         // BUT Gecko proxy application FAILS!
         coEvery { NetworkHardening.applyTorNetworkSettings(any(), any(), any(), any()) } returns false
 
-        val controller = PrivacyNetworkController(context, geckoEngine, torManager)
+        val controller = PrivacyNetworkController.getInstance(context)
         
         val result = controller.enterGhostMode("test_tab")
 
         // Action MUST fail
