package com.remmi.browser.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkRouteAuthorityTest {

  @Test
  fun testOnionDestinationDetection() {
    assertTrue(NetworkRouteAuthority.isOnionDestination("http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"))
    assertTrue(NetworkRouteAuthority.isOnionDestination("https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/path"))
    assertTrue(NetworkRouteAuthority.isOnionDestination("http://v2domain.onion:8080/test"))
    assertTrue(NetworkRouteAuthority.isOnionDestination("torproject.onion"))

    assertFalse(NetworkRouteAuthority.isOnionDestination("https://example.com"))
    assertFalse(NetworkRouteAuthority.isOnionDestination("https://onion.example.com"))
    assertFalse(NetworkRouteAuthority.isOnionDestination("https://google.com/search?q=onion"))
  }

  @Test
  fun testFailClosedWhenTorRequiredButUnavailable() {
    CurrentTorRoute.clearRoute()

    // When ghost is true and no socksPort is available, must throw IllegalStateException
    assertThrows(IllegalStateException::class.java) {
      NetworkRouteAuthority.createHttpClient(isGhost = true)
    }

    // createHttpClientOrNull should return null without crashing
    val nullClient = NetworkRouteAuthority.createHttpClientOrNull(isGhost = true)
    assertNull(nullClient)
  }

  @Test
  fun testClearnetClientCreationSucceeds() {
    CurrentTorRoute.clearRoute()

    val client = NetworkRouteAuthority.createHttpClient(isGhost = false)
    assertNotNull(client)
    assertNull(client.proxy)
  }

  @Test
  fun testGhostClientConfiguresSocksProxy() {
    CurrentTorRoute.updateRoute(
      socksPort = 9050,
      isGhostActive = true,
      isVerified = true,
      exitIp = "185.220.101.5"
    )

    val client = NetworkRouteAuthority.createHttpClient(isGhost = true)
    assertNotNull(client)
    assertNotNull(client.proxy)
    assertEquals(java.net.Proxy.Type.SOCKS, client.proxy?.type())

    CurrentTorRoute.clearRoute()
  }
}
