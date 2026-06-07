package com.neteasedecryptor.sdk

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object NetEaseDecryptor : IDecryptor {

    // 网易加密特定的魔数头部: [0x80, 0x1D, 0x30, 0x01]
    private val MAGIC_HEADER = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    private const val HEADER_SIZE = 4

    override fun isEncrypted(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) {
            return false
        }
        inputStream.mark(HEADER_SIZE)
        val header = ByteArray(HEADER_SIZE)
        val bytesRead = inputStream.read(header)
        inputStream.reset()

        if (bytesRead < HEADER_SIZE) return false
        return header.contentEquals(MAGIC_HEADER)
    }

    override fun deriveKey(currentStream: InputStream, manifestName: String): ByteArray {
        val allBytes = currentStream.use { it.readBytes() }
        if (allBytes.size < HEADER_SIZE) {
            throw IllegalArgumentException("CURRENT file is too small or invalid")
        }

        // 1. 验证魔数文件头
        val header = allBytes.copyOfRange(0, HEADER_SIZE)
        if (!header.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("CURRENT file is not encrypted by NetEase (Magic header mismatch)")
        }

        // 2. 截取除去头部的密文 Body
        val encryptedBody = allBytes.copyOfRange(HEADER_SIZE, allBytes.size)

        // 3. 将 MANIFEST 文件名转换为 UTF-8 字节并追加换行符 0x0A
        val manifestBytes = manifestName.toByteArray(StandardCharsets.UTF_8)
        val sourceBytes = ByteArray(manifestBytes.size + 1)
        manifestBytes.copyInto(sourceBytes)
        sourceBytes[sourceBytes.size - 1] = 0x0A.toByte()

        // 4. 将 CURRENT 密文与 Source Bytes 进行异或，推导原始密钥
        val rawKey = ByteArray(encryptedBody.size)
        for (i in encryptedBody.indices) {
            rawKey[i] = (encryptedBody[i].toInt() xor sourceBytes[i % sourceBytes.size].toInt()).toByte()
        }

        // 5. 密钥去重优化 (如果长度为 16 且前后八字节相同，则取 8 字节)
        return if (rawKey.size == 16) {
            val firstHalf = rawKey.copyOfRange(0, 8)
            val secondHalf = rawKey.copyOfRange(8, 16)
            if (firstHalf.contentEquals(secondHalf)) firstHalf else rawKey
        } else {
            rawKey
        }
    }

    override fun decryptFile(input: InputStream, output: OutputStream, key: ByteArray): Boolean {
        return try {
            input.use { inputStream ->
                output.use { outputStream ->
                    val fileData = inputStream.readBytes()

                    if (hasMagicHeader(fileData)) {
                        // 包含网易魔数头部：剥离前 4 字节，对 Body 数据循环进行 XOR 解密
                        val body = fileData.copyOfRange(HEADER_SIZE, fileData.size)
                        val decrypted = xor(body, key)
                        outputStream.write(decrypted)
                    } else {
                        // 未加密文件：直接原样写出
                        outputStream.write(fileData)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun hasMagicHeader(data: ByteArray): Boolean {
        if (data.size < HEADER_SIZE) return false
        for (i in 0 until HEADER_SIZE) {
            if (data[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    private fun xor(data: ByteArray, key: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return result
    }
}
