package com.github.cdahmad.cop

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WebScreen - 带 TopAppBar 的 WebView 页面
 *
 * @param title 页面标题，默认为 ""
 * @param url 要加载的网页 URL
 * @param onBackClick 返回按钮点击回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(
    title: String = "",
    url: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var webViewTitle by remember { mutableStateOf(title) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // 文件选择回调
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // 拍照临时 Uri
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    // 选择类型对话框
    var showChooseDialog by remember { mutableStateOf(false) }
    var acceptTypes by remember { mutableStateOf(arrayOf<String>()) }

    // WebView 引用
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 文件选择器 - 用于选择图片/视频
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    // 拍照 Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            filePathCallback?.onReceiveValue(arrayOf(cameraPhotoUri!!))
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // 录像 Launcher
    val takeVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && cameraVideoUri != null) {
            filePathCallback?.onReceiveValue(arrayOf(cameraVideoUri!!))
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            showChooseDialog = true
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    // 创建图片文件
    fun createImageFile(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_$timeStamp"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    // 创建视频文件
    fun createVideoFile(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoFileName = "VIDEO_$timeStamp"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File.createTempFile(videoFileName, ".mp4", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            videoFile
        )
    }

    // 处理文件选择
    fun openFileChooser(acceptTypeStrings: Array<String>) {
        acceptTypes = acceptTypeStrings

        // 检查权限
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val activity = context as? Activity
        val needsPermission = permissions.any {
            ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission && activity != null) {
            permissionLauncher.launch(permissions)
        } else {
            showChooseDialog = true
        }
    }

    // 选择对话框
    if (showChooseDialog) {
        val isImageType = acceptTypes.any {
            it.startsWith("image/") || it == "image/*"
        }
        val isVideoType = acceptTypes.any {
            it.startsWith("video/") || it == "video/*"
        }

        AlertDialog(
            onDismissRequest = {
                showChooseDialog = false
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            },
            title = { Text("选择文件") },
            text = { Text("请选择上传方式") },
            confirmButton = {
                TextButton(onClick = {
                    showChooseDialog = false
                    // 从文件选择
                    val mimeTypes = when {
                        isImageType && isVideoType -> arrayOf("image/*", "video/*")
                        isImageType -> arrayOf("image/*")
                        isVideoType -> arrayOf("video/*")
                        else -> acceptTypes
                    }
                    filePickerLauncher.launch(mimeTypes)
                }) {
                    Text("从文件选择")
                }
            },
            dismissButton = {
                Row {
                    if (isImageType || (!isImageType && !isVideoType)) {
                        TextButton(onClick = {
                            showChooseDialog = false
                            // 拍照
                            cameraPhotoUri = createImageFile()
                            cameraPhotoUri?.let { takePictureLauncher.launch(it) }
                        }) {
                            Text("拍照")
                        }
                    }
                    if (isVideoType || (!isImageType && !isVideoType)) {
                        TextButton(onClick = {
                            showChooseDialog = false
                            // 录像
                            cameraVideoUri = createVideoFile()
                            cameraVideoUri?.let { takeVideoLauncher.launch(it) }
                        }) {
                            Text("录像")
                        }
                    }
                    TextButton(onClick = {
                        showChooseDialog = false
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                    }) {
                        Text("取消")
                    }
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标题栏
        TopAppBar(
            title = {
                Text(
                    text = webViewTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (webViewRef?.canGoBack() == true) {
                        webViewRef?.goBack()
                    } else {
                        onBackClick()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            ),
            modifier = Modifier.statusBarsPadding()
        )

        // 加载进度条
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        //无法预览添加
        if (LocalInspectionMode.current) {
            Text(text = "网页内容")
        } else {
            // WebView 内容
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false

                        webViewClient = WebViewClient()

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrEmpty()) {
//                                webViewTitle = title
                                }
                            }

                            // 文件选择回调 (Android 5.0+ sdk23)
                            override fun onShowFileChooser(
                                webView: WebView?,
                                callback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                // 先清理之前的回调
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = callback

                                val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf()
                                val isCaptureEnabled = fileChooserParams?.isCaptureEnabled ?: false

                                // 如果是直接拍摄模式
                                if (isCaptureEnabled) {
                                    val isImageType = acceptTypes.any {
                                        it.startsWith("image/") || it == "image/*"
                                    }
                                    if (isImageType) {
                                        cameraPhotoUri = createImageFile()
                                        cameraPhotoUri?.let { takePictureLauncher.launch(it) }
                                    } else {
                                        cameraVideoUri = createVideoFile()
                                        cameraVideoUri?.let { takeVideoLauncher.launch(it) }
                                    }
                                } else {
                                    openFileChooser(acceptTypes)
                                }

                                return true
                            }
                        }

                        loadUrl(url)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    // 生命周期处理 - 页面销毁时清理回调
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.destroy()
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_4")
annotation class PreviewA


@PreviewA
@Composable
fun WebScreenPreview() {
    WebScreen(
        title = "网页标题",
        url = "https://www.baidu.com",
        onBackClick = {}
    )
}