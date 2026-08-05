package com.neteasedecryptor.android

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.neteasedecryptor.sdk.AbstractFile
import com.neteasedecryptor.sdk.JavaFileWrapper
import com.neteasedecryptor.sdk.WorldDecryptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AndroidWorldDecryptor(private val context: Context) {

    interface DecryptListener : WorldDecryptor.DecryptListener

    suspend fun decryptWorld(
        worldFolderDoc: DocumentFile,
        targetExportDir: File,
        listener: WorldDecryptor.DecryptListener
    ): String? = decryptWorld(DocumentFileWrapper(worldFolderDoc, context), targetExportDir, listener)

    suspend fun decryptWorld(
        worldFolderFile: File,
        targetExportDir: File,
        listener: WorldDecryptor.DecryptListener
    ): String? = decryptWorld(JavaFileWrapper(worldFolderFile), targetExportDir, listener)

    suspend fun decryptWorld(
        abstractWorldFolder: AbstractFile,
        targetExportDir: File,
        listener: WorldDecryptor.DecryptListener
    ): String? = withContext(Dispatchers.IO) {
        val mainScope = CoroutineScope(Dispatchers.Main)

        WorldDecryptor.decryptWorld(
            worldFolder = abstractWorldFolder,
            targetExportDir = targetExportDir,
            listener = object : WorldDecryptor.DecryptListener {
                override fun onProgress(progress: Int) {
                    mainScope.launch { listener.onProgress(progress) }
                }

                override fun onLog(message: String) {
                    mainScope.launch { listener.onLog(message) }
                }

                override fun onSuccess(exportPath: String) {
                    mainScope.launch { listener.onSuccess(exportPath) }
                }

                override fun onError(error: String) {
                    mainScope.launch { listener.onError(error) }
                }
            }
        )
    }
}
