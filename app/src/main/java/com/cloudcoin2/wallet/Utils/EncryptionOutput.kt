package com.cloudcoin2.wallet.Utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val AAD_LENGTH = 16
const val TAG_LENGTH = 16


class EncryptionOutput(val iv: ByteArray,
                       val tag: ByteArray,
                       val ciphertext: ByteArray)

fun encrypt(key: SecretKey, message: ByteArray): EncryptionOutput {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.iv.copyOf()
    val result = cipher.doFinal(message)
    val ciphertext = result.copyOfRange(0, result.size - TAG_LENGTH)
    val tag = result.copyOfRange(result.size - TAG_LENGTH, result.size)
    return EncryptionOutput(iv, tag, ciphertext)
}

fun decrypt(key: SecretKey, iv: ByteArray, tag: ByteArray, ciphertext: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(TAG_LENGTH * 8, iv)
    cipher.init(Cipher.DECRYPT_MODE, key, spec)
    return cipher.doFinal(ciphertext + tag)
}