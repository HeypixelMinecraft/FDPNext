package net.ccbluex.liquidbounce.skid.sigma

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NeteaseApiEncrypt {

    private val secureRandom = SecureRandom()

    fun encrypt(plainText: String): Array<String> {
        try {
            val secretKey = generateRandomKey(16)
            val firstEncrypted = aesEncrypt(plainText, NeteaseConstants.getPresetKey())
            val params = aesEncrypt(firstEncrypted, secretKey)
            val encSecKey = rsaEncrypt(secretKey)
            return arrayOf(params, encSecKey)
        } catch (e: Exception) {
            throw RuntimeException("Netease weapi encrypt failed", e)
        }
    }

    private fun aesEncrypt(plainText: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(NeteaseConstants.getIv().toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    private fun rsaEncrypt(text: String): String {
        val reversed = text.reversed()
        val hexText = StringBuilder()
        for (b in reversed.toByteArray()) {
            hexText.append(String.format("%02x", b))
        }

        val biText = BigInteger(hexText.toString(), 16)
        val biEx = BigInteger(NeteaseConstants.getPublicKey(), 16)
        val biMod = BigInteger(NeteaseConstants.getModulus(), 16)

        val biResult = biText.modPow(biEx, biMod)
        var result = biResult.toString(16)
        while (result.length < 256) {
            result = "0$result"
        }
        return result
    }

    private fun generateRandomKey(length: Int): String {
        val sb = StringBuilder(length)
        val charset = NeteaseConstants.getCharSet()
        for (i in 0 until length) {
            sb.append(charset[secureRandom.nextInt(charset.length)])
        }
        return sb.toString()
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }
}
