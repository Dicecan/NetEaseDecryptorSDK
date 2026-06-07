package com.neteasedecryptor.sdk

import java.io.InputStream
import java.io.OutputStream

/**
 * 核心解密接口，解耦 Android SDK 的具体实现，支持纯 JVM 开发环境
 */
interface IDecryptor {

    /**
     * 判断输入流是否包含网易加密魔数首部
     * @param inputStream 输入流
     * @return 是否已加密
     */
    fun isEncrypted(inputStream: InputStream): Boolean

    /**
     * 从加密的 CURRENT 文件与指定的 MANIFEST 文件名中推导对称异或密钥
     * @param currentStream CURRENT 文件的输入流
     * @param manifestName db 目录下当前有效的 MANIFEST 文件全名（例如: MANIFEST-000001）
     * @return 最终解密使用的 Key 字节数组
     */
    fun deriveKey(currentStream: InputStream, manifestName: String): ByteArray

    /**
     * 使用推导的密钥解密输入流，并将结果写入输出流
     * @param input 源加密文件输入流
     * @param output 解密后文件输出流
     * @param key 解密密钥字节数组
     * @return 是否成功解密或复制
     */
    fun decryptFile(input: InputStream, output: OutputStream, key: ByteArray): Boolean
}
