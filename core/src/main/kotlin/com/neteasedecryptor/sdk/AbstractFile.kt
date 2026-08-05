package com.neteasedecryptor.sdk

import java.io.InputStream

interface AbstractFile {
    val name: String
    val isDirectory: Boolean
    val isFile: Boolean

    fun listFiles(): List<AbstractFile>

    fun findFile(name: String): AbstractFile? {
        return listFiles().find { it.name == name }
    }

    fun openInputStream(): InputStream?
}
