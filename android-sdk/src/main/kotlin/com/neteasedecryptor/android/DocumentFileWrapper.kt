package com.neteasedecryptor.android

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.neteasedecryptor.sdk.AbstractFile
import java.io.InputStream

class DocumentFileWrapper(
    private val docFile: DocumentFile,
    private val context: Context
) : AbstractFile {

    override val name: String get() = docFile.name ?: ""
    override val isDirectory: Boolean get() = docFile.isDirectory
    override val isFile: Boolean get() = docFile.isFile

    override fun listFiles(): List<AbstractFile> {
        return docFile.listFiles().map { DocumentFileWrapper(it, context) }
    }

    override fun findFile(name: String): AbstractFile? {
        val target = docFile.findFile(name) ?: return null
        return DocumentFileWrapper(target, context)
    }

    override fun openInputStream(): InputStream? {
        return context.contentResolver.openInputStream(docFile.uri)
    }
}
