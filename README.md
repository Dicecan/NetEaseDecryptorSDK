# NetEase Minecraft PE World Decryptor SDK

一个用于解密网易版《我的世界》基岩版（Minecraft PE）存档的 Kotlin 开源 SDK。

它可以帮助开发者方便地逆向和解密网易专有的 LevelDB 存档格式，以便导出为标准的国际版基岩版格式。

---

## 📦 依赖引入 (Dependency Setup - JitPack)

本 SDK 已托管至 **JitPack** 仓库，最新版本为 `2.0.0`。

### 步骤 1: 配置仓库地址
在项目根目录的 `settings.gradle.kts` 中配置 JitPack 镜像源：

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 步骤 2: 添加模块依赖
在应用的 `build.gradle.kts` 中根据需求引入对应的依赖项：

```kotlin
dependencies {
    // 选项 A: Android 端完整 SDK (推荐：包含 DocumentFile (SAF) 适配、协程异步支持及 Shizuku/Root 物理路径访问)
    implementation("com.github.Dicecan.NetEaseDecryptorSDK:android-sdk:2.0.0")

    // 选项 B: 纯 JVM 核心模块 (适用：PC 工具、桌面端 Java 应用或服务端，没有任何 Android 框架依赖)
    implementation("com.github.Dicecan.NetEaseDecryptorSDK:core:2.0.0")
}
```

---

## 🛠️ 项目模块结构与优化特性

项目采用多模块设计，将纯逻辑层与 Android 依赖解耦：

* **`:core`**：纯 Kotlin/JVM 实现，没有任何外部依赖。支持**流式分块解密 (Chunked Stream Decryption)** 与**就地异或 (In-place XOR)**，极致优化内存占用与 CPU 缓存命中率，彻底避免大型 LevelDB 日志文件解密导致的 OOM 问题。同时提供 `AbstractFile` 接口实现文件系统抽象。
* **`:android-sdk`**：Android 端专用适配层。除了支持在传统 SAF 授权模式下使用 `DocumentFile` 目录外，还**完美适配 Android 15+ 平台**——在使用 **Shizuku / Root** 框架获取到 `/Android/data/` 物理目录访问权限时，可以直接传入原生 `java.io.File` 路径进行解密。

---

## 🚀 快速使用 (Quick Start)

### 1. JVM 或 PC 应用集成 (`:core` 模块)

#### 单文件解密
对于桌面应用或不需要 Android 依赖的场景，可以直接通过流接口操作：

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

// 3. 对目标加密文件执行流式解密输出
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

#### 整存档解密
在纯 JVM 环境下，也可以使用 `WorldDecryptor` 和 `JavaFileWrapper` 解密整个存档：

```kotlin
import com.neteasedecryptor.sdk.WorldDecryptor
import com.neteasedecryptor.sdk.JavaFileWrapper
import java.io.File

val worldDir = JavaFileWrapper(File("/path/to/netease/world"))
val exportDir = File("/path/to/export/world")

WorldDecryptor.decryptWorld(
    worldFolder = worldDir,
    targetExportDir = exportDir,
    listener = object : WorldDecryptor.DecryptListener {
        override fun onProgress(progress: Int) { println("Progress: $progress%") }
        override fun onLog(message: String) { println(message) }
        override fun onSuccess(exportPath: String) { println("Exported to $exportPath") }
        override fun onError(error: String) { println("Error: $error") }
    }
)
```

---

### 2. Android 客户端集成 (`:android-sdk` 模块)

#### 方式 A：传统 SAF (`DocumentFile`) 模式 (Android 10 - 13)
```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.documentfile.provider.DocumentFile
import com.neteasedecryptor.android.AndroidWorldDecryptor
import kotlinx.coroutines.launch
import java.io.File

val androidDecryptor = AndroidWorldDecryptor(context)

lifecycleScope.launch {
    // worldFolderDoc 是用户通过 SAF 选择授权的 DocumentFile 根目录
    val worldFolderDoc = DocumentFile.fromTreeUri(context, treeUri)!!
    val targetExportDir = File(context.getExternalFilesDir(null), "DecryptedWorld")

    androidDecryptor.decryptWorld(
        worldFolderDoc = worldFolderDoc,
        targetExportDir = targetExportDir,
        listener = object : AndroidWorldDecryptor.DecryptListener {
            override fun onProgress(progress: Int) { progressBar.progress = progress }
            override fun onLog(message: String) { logTextView.append("\n$message") }
            override fun onSuccess(exportPath: String) { Toast.makeText(context, "解密成功: $exportPath", Toast.LENGTH_LONG).show() }
            override fun onError(error: String) { Toast.makeText(context, "错误: $error", Toast.LENGTH_SHORT).show() }
        }
    )
}
```

#### 方式 B：Android 15+ Shizuku / Root 模式 (`java.io.File`)
在 Android 14/15+ 上，系统封禁了 SAF 对 `/Android/data` 的访问授权。通过 Shizuku 提升权限或 Root 挂载获取到存档物理路径后，可以直接使用 `File` 重载：

```kotlin
lifecycleScope.launch {
    // 通过 Shizuku API / Root 挂载访问到的物理存档目录
    val shizukuWorldFile = File("/storage/emulated/0/Android/data/com.netease.x19/files/minecraftWorlds/xxx")
    val targetExportDir = File(context.getExternalFilesDir(null), "DecryptedWorld")

    androidDecryptor.decryptWorld(
        worldFolderFile = shizukuWorldFile,
        targetExportDir = targetExportDir,
        listener = object : AndroidWorldDecryptor.DecryptListener {
            override fun onProgress(progress: Int) { progressBar.progress = progress }
            override fun onLog(message: String) { logTextView.append("\n$message") }
            override fun onSuccess(exportPath: String) { Toast.makeText(context, "解密成功: $exportPath", Toast.LENGTH_LONG).show() }
            override fun onError(error: String) { Toast.makeText(context, "错误: $error", Toast.LENGTH_SHORT).show() }
        }
    )
}
```

---

## ⚖️ 开源协议 (License)

本 SDK 遵循 **GPL v3.0** 开源许可协议。在二开或引用本类库代码时，必须保持衍生库的开源。请勿将此 SDK 用于恶意商业篡改、倒卖用户存档或外挂工具等非法用途。
