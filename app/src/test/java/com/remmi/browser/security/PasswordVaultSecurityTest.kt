package com.remmi.browser.security

import com.remmi.browser.security.crypto.PasswordCryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class PasswordVaultSecurityTest {

  @Test
  fun testPinValidation() {
    assertTrue(PasswordCryptoEngine.isPinValid("749182".toCharArray()))
    assertTrue(PasswordCryptoEngine.isPinValid("93847281".toCharArray()))
    assertFalse(PasswordCryptoEngine.isPinValid("123456".toCharArray())) // trivial sequential pattern
    assertFalse(PasswordCryptoEngine.isPinValid("9876".toCharArray())) // too short (< 6 digits)
    assertFalse(PasswordCryptoEngine.isPinValid("123".toCharArray())) // too short
    assertFalse(PasswordCryptoEngine.isPinValid("74918a".toCharArray())) // non-digit
  }

  @Test
  fun testKeyDerivationAndAesGcmRoundtrip() {
    val masterPassword = "TestMasterPassword!2026".toCharArray()
    val kdfResult = PasswordCryptoEngine.deriveKeyEncryptionKey(masterPassword)

    val testData = "super_sensitive_api_key_42".toByteArray(StandardCharsets.UTF_8)
    val ciphertext = PasswordCryptoEngine.encryptAesGcm(kdfResult.kek, testData)

    val decrypted = PasswordCryptoEngine.decryptAesGcm(kdfResult.kek, ciphertext)
    assertEquals("super_sensitive_api_key_42", String(decrypted, StandardCharsets.UTF_8))
  }

  @Test
  fun testVerifierComputation() {
    val masterPassword = "VaultAccessKey#999".toCharArray()
    val kdfResult = PasswordCryptoEngine.deriveKeyEncryptionKey(masterPassword)

    val (verifier, salt) = PasswordCryptoEngine.computeVerifier(kdfResult.kek)
    assertTrue(PasswordCryptoEngine.verifyKek(kdfResult.kek, verifier, salt))

    val wrongKek = PasswordCryptoEngine.generateSecureRandomBytes(32)
    assertFalse(PasswordCryptoEngine.verifyKek(wrongKek, verifier, salt))
  }
}
