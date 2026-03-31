

# ComposeBase

一个包含多个实用模块的Android基础库，提供Compose UI组件和OkHttp拦截器等功能。

## 模块介绍

### 1. swaglog - OkHttp日志拦截器

一个功能强大的OkHttp日志拦截器，支持Swagger文档集成和JSON反混淆。

#### 功能特性
- ✅ 格式化输出请求和响应日志
- ✅ 支持JSON反混淆
- ✅ 集成Swagger文档
- ✅ 自动缓存Swagger文档
- ✅ 支持大文件日志分段
- ✅ 可自定义日志记录方式

#### 安装
```kotlin
implementation("com.github.cdAhmad:swagger-interceptor:1.0.7.6")
```

#### 使用示例

```kotlin
// 创建拦截器
val swaggerInterceptor = SwiggerLoggingInterceptor(
    baseUrl = "https://api.github.com",
    swaggerDocUrl = "/v2/api-docs",
    deobfus = true,  // 启用反混淆
    filter = true,   // 启用过滤
    format = true,   // 启用格式化
    cacheFile = { File(context.cacheDir, "swagger.json") },  // 缓存文件路径
    log = { level, tag, msg ->  // 自定义日志记录
        when (level) {
            0 -> Log.d(tag, msg)
            1 -> Log.e(tag, msg)
        }
    }
)

// 或者使用扩展函数
val okHttpClient = OkHttpClient.Builder()
    .addSwiggerLoggingInterceptor(
        filter = true,
        format = true,
        url = "https://api.github.com",
        cacheFile = { File(context.cacheDir, "swagger.json") },
        log = { level, tag, msg -> Log.d(tag, msg) }
    )
    .build()
```

### 2. cop - Compose UI组件库

一个包含常用Compose UI组件的库，简化Android应用开发。

#### 功能特性

##### TextCompose
- 基础文本组件

##### AnimCompose
- 带动画效果的文本组件
- 支持点击缩放动画

##### AnimatableCompose
- 更复杂的动画效果
- 支持弹簧动画

##### WebScreen
- 完整的WebView封装
- 支持顶部导航栏
- 支持加载进度显示
- 支持文件上传功能
- 支持拍照和录像
- 适配Android不同版本的权限

#### 安装
```kotlin
implementation("com.github.cdAhmad:compose-base:latest")
```

#### 使用示例

##### WebScreen组件

```kotlin
WebScreen(
    title = "网页标题",
    url = "https://www.baidu.com",
    onBackClick = { /* 返回按钮点击处理 */ }
)
```

##### AnimCompose组件

```kotlin
AnimCompose(default = true)
```

##### AnimatableCompose组件

```kotlin
AnimatableCompose(default = true)
```

## 项目结构

```
ComposeBase/
├── cop/                 # Compose UI组件库
│   ├── src/main/java/com/github/cdahmad/cop/
│   │   ├── Compose1.kt  # 基础Compose组件
│   │   └── WebCompose.kt # WebView组件
├── swaglog/             # OkHttp日志拦截器
│   ├── src/main/java/com/cdahmad/swiggerlog/
│   │   └── SwiggerLoggingInterceptor.kt
├── build.gradle.kts     # 项目配置
└── settings.gradle.kts  # 模块配置
```

## 技术栈

- **Kotlin** - 主要开发语言
- **Jetpack Compose** - UI框架
- **OkHttp** - 网络库
- **Gson** - JSON解析
- **Coroutines** - 异步处理

## 许可证

MIT License
