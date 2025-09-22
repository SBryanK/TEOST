package com.example.teost.core.data.util

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object ClientSideEncryptor {
    data class Encrypted(
        val algorithm: String,
        val keyBase64: String,
        val ivBase64: String,
        val cipherTextBase64: String
    )

    private const val ALGO = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    fun generateKey(): SecretKey {
        val kg = KeyGenerator.getInstance(ALGO)
        kg.init(256)
        return kg.generateKey()
    }

    fun encryptAesGcm(plaintext: ByteArray): Encrypted {
        val key = generateKey()
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return Encrypted(
            algorithm = "AES-256-GCM",
            keyBase64 = Base64.encodeToString(key.encoded, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            cipherTextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    fun encryptAesGcmWithKey(plaintext: ByteArray, keyBase64: String, ivBase64: String): Encrypted {
        val keyBytes = Base64.decode(keyBase64, Base64.DEFAULT)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        val key = SecretKeySpec(keyBytes, ALGO)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        return Encrypted(
            algorithm = "AES-256-GCM",
            keyBase64 = keyBase64,
            ivBase64 = ivBase64,
            cipherTextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }
}


