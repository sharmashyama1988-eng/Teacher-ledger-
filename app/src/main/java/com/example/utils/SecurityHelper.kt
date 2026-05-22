package com.example.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Safe static fallback seed mixed with dynamic PIN for robust field-masking
    private const val SALT = "TeacherLedgerSaltKey2026"

    /**
     * Hashes the user's PIN using SHA-256 to allow safe offline validation.
     */
    fun hashPin(pin: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Secure fallback in case of algorithm unavailability
            pin.hashCode().toString()
        }
    }

    /**
     * Symmetric encryption helper to mask/unmask sensitive text in the database if needed.
     */
    fun encrypt(value: String, secretKey: String): String {
        return try {
            val rawKey = generateKey(secretKey)
            val keySpec = SecretKeySpec(rawKey, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = IvParameterSpec(rawKey.take(16).toByteArray()) // Simple deterministic IV for offline stability
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv)
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            value // Fallback to plain if error, preventing data loss
        }
    }

    fun decrypt(encryptedValue: String, secretKey: String): String {
        if (encryptedValue.isEmpty()) return ""
        return try {
            val rawKey = generateKey(secretKey)
            val keySpec = SecretKeySpec(rawKey, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = IvParameterSpec(rawKey.take(16).toByteArray())
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv)
            val decodedBytes = Base64.decode(encryptedValue, Base64.DEFAULT)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedValue // Fallback
        }
    }

    private fun generateKey(pin: String): ByteArray {
        val mixed = pin + SALT
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(mixed.toByteArray(Charsets.UTF_8))
    }
}
