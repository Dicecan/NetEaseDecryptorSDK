# NetEase Minecraft PE World Decryptor SDK

一个用于解密网易版《我的世界》基岩版（Minecraft PE）存档的 Kotlin 开源 SDK。

它可以帮助开发者方便地逆向和解密网易专有的 LevelDB 存档格式，以便导出为标准的国际版基岩版格式。

---

## 🛠️ 项目模块结构

项目采用多模块设计，将纯逻辑层与 Android 依赖解耦：

* **`:core`**：纯 Kotlin/JVM 实现，没有任何外部依赖。可在服务器、PC 工具或 Android 平台无缝复用。
* **`:android-sdk`**：Android 端专用适配层，集成 `androidx.documentfile`，支持在 Android 10+ 的分区存储模式下直接处理经由 SAF 授权授权的 `Uri` 和 `DocumentFile` 目录。

---

## 🚀 快速使用 (Quick Start)

### 1. JVM 或 PC 应用集成 (`:core` 模块)

对于桌面应用或不需要 Android 依赖的场景，可以直接通过 Java Stream 接口操作：

```kotlin
import com.neteasedecryptor.sdk.NetEaseDecryptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// 1. 获取 db 目录及关键文件
val dbDir = File("/path/to/world/db")
val currentFile = File(dbDir, "CURRENT")
val manifestFile = dbDir.listFiles()?.find { it.name.startsWith("MANIFEST") } ?: error("No MANIFEST found")

// 2. 利用 CURRENT 和 MANIFEST 的名字推导解密 Key
val currentStream = FileInputStream(currentFile)
val decryptKey = NetEaseDecryptor.deriveKey(currentStream, manifestFile.name)

// 3. 对目标加密文件执行解密输出
val encryptedFile = File(dbDir, "000003.log")
val decryptedOutputFile = File("/path/to/output/000003.log")

val isOk = NetEaseDecryptor.decryptFile(
    input = FileInputStream(encryptedFile),
    output = FileOutputStream(decryptedOutputFile),
    key = decryptKey
)
if (isOk) {
    println("解密文件成功！")
}
```

### 2. Android 客户端集成 (`:android-sdk` 模块)

在 Android 应用中，使用 Storage Access Framework（SAF）选择存档目录，并利用 `AndroidWorldDecryptor` 进行异步后台解密：

```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.documentfile.provider.DocumentFile
import com.neteasedecryptor.android.AndroidWorldDecryptor
import kotlinx.coroutines.launch
import java.io.File

// 获取 Android 平台解密器实例
val androidDecryptor = AndroidWorldDecryptor(context)

lifecycleScope.launch {
    // worldFolderDoc 是用户通过 SAF 选择授权的 DocumentFile 根目录
    val worldFolderDoc = DocumentFile.fromTreeUri(context, treeUri)!!
    
    // 准备本地的导出绝对路径目标
    val targetExportDir = File(context.getExternalFilesDir(null), "DecryptedWorld")

    androidDecryptor.decryptWorld(
        worldFolderDoc = worldFolderDoc,
        targetExportDir = targetExportDir,
        listener = object : AndroidWorldDecryptor.DecryptListener {
            override fun onProgress(progress: Int) {
                // 更新 UI 进度条百分比 (0-100)
                progressBar.progress = progress
            }

            override fun onLog(message: String) {
                // 打印解密日志
                logTextView.append("\n$message")
            }

            override fun onSuccess(exportPath: String) {
                // 导出完成
                Toast.makeText(context, "解密成功，已导出至: $exportPath", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: String) {
                // 出错处理
                Toast.makeText(context, "解密错误: $error", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
```

---

## ⚖️ 开源协议 (License)

本 SDK 遵循 **GPL v3.0** 开源许可协议。在二开或引用本类库代码时，必须保持衍生库的开源。请勿将此 SDK 用于恶意商业篡改、倒卖用户存档或外挂工具等非法用途。
