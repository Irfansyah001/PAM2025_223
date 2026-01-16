package com.umy.medremindid.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

object PasswordHasher {

    private const val ALGO = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    // format: pbkdf2$iterations$saltBase64$hashBase64
    fun hash(password: CharArray): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)

        val hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        return "pbkdf2$$ITERATIONS$$saltB64$$hashB64"
    }

    fun verify(password: CharArray, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4) return false
        val algo = parts[0]
        if (algo != "pbkdf2") return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = Base64.decode(parts[2], Base64.NO_WRAP)
        val expected = Base64.decode(parts[3], Base64.NO_WRAP)

        val actual = pbkdf2(password, salt, iterations, expected.size * 8)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLenBits: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLenBits)
        val skf = SecretKeyFactory.getInstance(ALGO)
        return skf.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}
