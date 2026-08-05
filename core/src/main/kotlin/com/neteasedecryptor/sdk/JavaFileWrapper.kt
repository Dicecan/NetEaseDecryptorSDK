package com.neteasedecryptor.sdk

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class JavaFileWrapper(val file: File) : AbstractFile {

    override val name: String get() = file.name
    override val isDirectory: Boolean get() = file.isDirectory
    override val isFile: Boolean get() = file.isFile

    override fun listFiles(): List<AbstractFile> {
        val children = file.listFiles() ?: return emptyList()
        return children.map { JavaFileWrapper(it) }
    }

    override fun findFile(name: String): AbstractFile? {
        val target = File(file, name)
        return if (target.exists()) JavaFileWrapper(target) else null
    }

    override fun openInputStream(): InputStream? {
        if (!file.exists() || !file.isFile) return null
        return FileInputStream(file)
    }
}
