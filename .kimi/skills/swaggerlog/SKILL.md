---
name: swaggerlog
description: Android OkHttp 日志拦截器库，支持 Swagger 文档集成、JSON 反混淆和格式化日志输出。用于在 Android 项目中集成网络请求日志调试功能，支持 Debug/Release 环境区分、自动缓存 Swagger 文档、大文件分段处理。
---

# SwaggerLog

功能强大的 Android OkHttp 日志拦截器，简化网络调试。

## 快速开始

### 1. 添加依赖

在 `build.gradle.kts` 中添加 JitPack 仓库和依赖：

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    debugImplementation("com.github.cdAhmad:swaggerlog:1.1.3")
}
```

### 2. 创建 LogHelper（推荐做法）

**Debug 版本** (`app/src/debug/kotlin/LogHelper.kt`)：

```kotlin
import com.cdahmad.swaggerlog.SwaggerLoggingInterceptor
import okhttp3.Interceptor
import java.io.File

object LogHelper {
    fun getInterceptor(
        apiUrl: String,
        format: Boolean,
        cacheFile: () -> File,
        log: (level: Int, tag: String, msg: String) -> Unit
    ): Interceptor? {
        return SwaggerLoggingInterceptor(
            baseUrl = apiUrl,
            swaggerDocUrl = "${apiUrl}v2/api-docs",
            deobfus = true,    // 启用 JSON 反混淆
            filter = true,     // 启用日志过滤
            format = format,   // 启用格式化输出
            cacheFile = cacheFile,
            log = log
        )
    }
}
```

**Release 版本** (`app/src/release/java/LogHelper.kt`)：

```kotlin
import okhttp3.Interceptor
import java.io.File

object LogHelper {
    fun getInterceptor(
        apiUrl: String,
        format: Boolean,
        cacheFile: () -> File,
        log: (level: Int, tag: String, msg: String) -> Unit
    ): Interceptor? {
        return null  // Release 环境不记录日志
    }
}
```

### 3. 集成到 OkHttpClient

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .apply {
        LogHelper.getInterceptor(
            apiUrl = "https://api.example.com",
            format = true,
            cacheFile = { File(context.cacheDir, "swagger.json") },
            log = { level, tag, msg -> Log.d(tag, msg) }
        )?.let { addInterceptor(it) }
    }
    .build()
```

## 核心功能

| 功能 | 说明 |
|------|------|
| 格式化日志 | 请求/响应日志格式化输出 |
| JSON 反混淆 | 自动还原混淆后的 JSON 字段名 |
| Swagger 集成 | 自动获取并缓存 Swagger 文档 |
| 本地缓存 | 24 小时 TTL 缓存 Swagger 文档 |
| 大文件处理 | 超过 1MB 自动分段，避免 OOM |

## SwaggerLoggingInterceptor 参数

```kotlin
class SwaggerLoggingInterceptor(
    val baseUrl: String,           // API 基础 URL
    val swaggerDocUrl: String,     // Swagger 文档 URL
    val deobfus: Boolean,          // 是否反混淆 JSON
    val filter: Boolean,           // 是否过滤日志
    val format: Boolean = false,   // 是否格式化输出
    val cacheFile: () -> File?,    // 缓存文件提供者
    val log: (Int, String, String) -> Unit  // 日志回调
)
```

## 扩展函数用法

```kotlin
val client = OkHttpClient.Builder()
    .addSwaggerLoggingInterceptor(
        filter = true,
        format = true,
        url = "https://api.example.com",
        cacheFile = { File(context.cacheDir, "swagger.json") },
        log = { level, tag, msg -> Log.d(tag, msg) }
    )
    .build()
```

## 多 BuildType 支持

如有 staging、beta 等环境，在对应目录创建 LogHelper：

```
app/src/
├── debug/kotlin/LogHelper.kt     # 启用日志
├── staging/java/LogHelper.kt     # 复用 Release 版本
├── beta/java/LogHelper.kt        # 复用 Release 版本
└── release/java/LogHelper.kt     # 禁用日志
```

## 日志输出示例

```
OkHttp_000001  -> GET https://api.example.com/user/info
OkHttp_000001  <- GET https://api.example.com/user/info 200 (125 ms) OK
OkHttp_000001  <- Swagger: 获取用户信息 https://api.example.com/doc.html#/default/User/getUserInfo
OkHttp_000001  -> Request Header: {...}
OkHttp_000001  -> Request Body: null
OkHttp_000001  <- Response Body: {"a":"张三","b":25}
OkHttp_000001  <- Response Body (Deobfuscated): {"name":"张三","age":25}
```

## 注意事项

- 仅通过 JitPack 分发，确保添加 JitPack 仓库
- Debug 环境建议开启 `deobfus` 和 `format`
- Release 环境务必返回 `null`，避免性能损耗
- 缓存文件路径需有写入权限
