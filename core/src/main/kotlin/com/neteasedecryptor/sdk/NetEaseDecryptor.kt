package com.neteasedecryptor.sdk

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object NetEaseDecryptor : IDecryptor {

    // 网易加密文件头特征魔数 [0x80, 0x1D, 0x30, 0x01]
    private val MAGIC_HEADER = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    private const val HEADER_SIZE = 4
    private const val BUFFER_SIZE = 8192

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

    /**
     * 通过 CURRENT 密文 Body 与 MANIFEST 文件名异或推导解密 Key
     */
    override fun deriveKey(currentStream: InputStream, manifestName: String): ByteArray {
        val allBytes = currentStream.use { it.readBytes() }
        if (allBytes.size < HEADER_SIZE) {
            throw IllegalArgumentException("CURRENT file is too small or invalid")
        }

        // 验证 4 字节魔数头部
        val header = allBytes.copyOfRange(0, HEADER_SIZE)
        if (!header.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("CURRENT file is not encrypted by NetEase (Magic header mismatch)")
        }

        // 取出密文 Body
        val encryptedBody = allBytes.copyOfRange(HEADER_SIZE, allBytes.size)

        // 构造源明文字符串：MANIFEST文件名 + \n (0x0A)
        val manifestBytes = manifestName.toByteArray(StandardCharsets.UTF_8)
        val sourceBytes = ByteArray(manifestBytes.size + 1)
        manifestBytes.copyInto(sourceBytes)
        sourceBytes[sourceBytes.size - 1] = 0x0A.toByte()

        // 密文与明文异或，恢复 Key
        val rawKey = ByteArray(encryptedBody.size)
        for (i in encryptedBody.indices) {
            rawKey[i] = (encryptedBody[i].toInt() xor sourceBytes[i % sourceBytes.size].toInt()).toByte()
        }

        // 若推导出的 16 字节 Key 前后 8 字节重复，则取前 8 字节
        return if (rawKey.size == 16) {
            val firstHalf = rawKey.copyOfRange(0, 8)
            val secondHalf = rawKey.copyOfRange(8, 16)
            if (firstHalf.contentEquals(secondHalf)) firstHalf else rawKey
        } else {
            rawKey
        }
    }

    /**
     * 流式异或解密：跳过前 4 字节魔数，对其余内容循环 XOR 解密
     */
    override fun decryptFile(input: InputStream, output: OutputStream, key: ByteArray): Boolean {
        return try {
            input.use { inputStream ->
                output.use { outputStream ->
                    val header = ByteArray(HEADER_SIZE)
                    var headerBytesRead = 0
                    while (headerBytesRead < HEADER_SIZE) {
                        val read = inputStream.read(header, headerBytesRead, HEADER_SIZE - headerBytesRead)
                        if (read == -1) break
                        headerBytesRead += read
                    }

                    if (headerBytesRead == HEADER_SIZE && hasMagicHeader(header)) {
                        // 加密文件：逐块 XOR 解密写出
                        val buffer = ByteArray(BUFFER_SIZE)
                        var absoluteOffset = 0
                        while (true) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break

                            for (i in 0 until bytesRead) {
                                buffer[i] = (buffer[i].toInt() xor key[absoluteOffset % key.size].toInt()).toByte()
                                absoluteOffset++
                            }
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    } else {
                        // 未加密文件：原样写出
                        if (headerBytesRead > 0) {
                            outputStream.write(header, 0, headerBytesRead)
                        }
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            outputStream.write(buffer, 0, bytesRead)
                        }
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
