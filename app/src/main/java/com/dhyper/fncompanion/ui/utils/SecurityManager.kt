package com.dhyper.fncompanion.ui.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object SecurityManager {
    private const val KEY_ALIAS = "FortniteCompanionKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val EXPORT_ALGO = "AES/CBC/PKCS5Padding"
    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    init {
        generateKeystoreKey()
    }

    private fun generateKeystoreKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getKeystoreKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    // --- LOCAL KEYSTORE ENCRYPTION ---
    fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, getKeystoreKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray())
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(ciphertext: String?): String? {
        if (ciphertext == null) return null
        try {
            val combined = Base64.decode(ciphertext, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until 12)
            val encrypted = combined.sliceArray(12 until combined.size)
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, getKeystoreKey(), GCMParameterSpec(128, iv))
            return String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            return null
        }
    }

    // --- PASSWORD BASED ENCRYPTION FOR EXPORT ---
    fun encryptWithPassword(plaintext: String, password: CharArray): String {
        val salt = ByteArray(SALT_SIZE).apply { SecureRandom().nextBytes(this) }
        val secretKey = deriveKey(password, salt)
        
        val cipher = Cipher.getInstance(EXPORT_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray())
        
        val combined = salt + iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decryptWithPassword(ciphertext: String, password: CharArray): String? {
        try {
            val combined = Base64.decode(ciphertext, Base64.DEFAULT)
            val salt = combined.sliceArray(0 until SALT_SIZE)
            val iv = combined.sliceArray(SALT_SIZE until SALT_SIZE + IV_SIZE)
            val encrypted = combined.sliceArray(SALT_SIZE + IV_SIZE until combined.size)
            
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(EXPORT_ALGO)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            return String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            return null
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = javax.crypto.SecretKeyFactory.getInstance(PBKDF2_ALGO)
        val spec = PBEKeySpec(password, salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
