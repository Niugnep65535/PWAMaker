package tw.idv.niugnep.pwamaker.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import tw.idv.niugnep.pwamaker.R
import tw.idv.niugnep.pwamaker.model.PwaConfig
import tw.idv.niugnep.pwamaker.utils.ShortcutUtils

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ViewerScreen(
    config: PwaConfig,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Update Activity Task Description so it shows the PWA title & icon in Android Recents Overview
    LaunchedEffect(config) {
        if (activity != null) {
            val iconBitmap = ShortcutUtils.loadOrGenerateIcon(activity, config)
            @Suppress("DEPRECATION")
            activity.setTaskDescription(
                ActivityManager.TaskDescription(config.name, iconBitmap)
            )
        }
    }

    // WebView reference and state
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    // Fullscreen view overlay (for web videos)
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Handlers for Web Chrome Client async callbacks
    var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingFilePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<Pair<String, GeolocationPermissions.Callback>?>(null) }

    // Activity Result Launcher for Hardware Permissions (Camera & Microphone)
    val webPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val request = pendingWebPermissionRequest
        if (request != null) {
            val grantedResources = mutableListOf<String>()
            for (resource in request.resources) {
                when (resource) {
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                        if (permissionsMap[Manifest.permission.RECORD_AUDIO] == true) {
                            grantedResources.add(resource)
                        }
                    }
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                        if (permissionsMap[Manifest.permission.CAMERA] == true) {
                            grantedResources.add(resource)
                        }
                    }
                    else -> grantedResources.add(resource)
                }
            }
            if (grantedResources.isNotEmpty()) {
                request.grant(grantedResources.toTypedArray())
            } else {
                request.deny()
            }
            pendingWebPermissionRequest = null
        }
    }

    // Activity Result Launcher for File Upload (<input type="file">)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFilePathCallback
        if (callback != null) {
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data
                val uris = mutableListOf<Uri>()
                if (data?.data != null) {
                    uris.add(data.data!!)
                } else if (data?.clipData != null) {
                    val clip = data.clipData!!
                    for (i in 0 until clip.itemCount) {
                        uris.add(clip.getItemAt(i).uri)
                    }
                }
                callback.onReceiveValue(uris.toTypedArray())
            } else {
                callback.onReceiveValue(null)
            }
            pendingFilePathCallback = null
        }
    }

    // Activity Result Launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val geo = pendingGeoCallback
        if (geo != null) {
            val isGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            geo.second.invoke(geo.first, isGranted, false)
            pendingGeoCallback = null
        }
    }

    // Handle back gesture / back button
    BackHandler(enabled = true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onClose()
            activity?.moveTaskToBack(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // WebView container
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure WebView Settings
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        setGeolocationEnabled(true)
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        userAgentString = config.getEffectiveUserAgent()
                    }

                    // WebViewClient implementation
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                return false // Load in WebView
                            }
                            // External intents (tel:, mailto:, intent://, etc.)
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(ctx, ctx.getString(R.string.toast_cannot_open_external), Toast.LENGTH_SHORT).show()
                            }
                            return true
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.toast_web_error, error?.description ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    // WebChromeClient implementation
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadingProgress = newProgress / 100f
                            if (newProgress >= 100) isLoading = false
                        }

                        // WebRTC / getUserMedia permission request (Camera & Microphone)
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            if (request == null) return
                            val permissionsToRequest = mutableListOf<String>()

                            for (resource in request.resources) {
                                if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                                    if (ContextCompat.checkSelfPermission(
                                            ctx,
                                            Manifest.permission.RECORD_AUDIO
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                                    if (ContextCompat.checkSelfPermission(
                                            ctx,
                                            Manifest.permission.CAMERA
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionsToRequest.add(Manifest.permission.CAMERA)
                                    }
                                }
                            }

                            if (permissionsToRequest.isNotEmpty()) {
                                pendingWebPermissionRequest = request
                                webPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                            } else {
                                request.grant(request.resources)
                            }
                        }

                        // HTML <input type="file"> support
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            pendingFilePathCallback?.onReceiveValue(null)
                            pendingFilePathCallback = filePathCallback

                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }

                            try {
                                filePickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                pendingFilePathCallback = null
                                Toast.makeText(ctx, ctx.getString(R.string.toast_cannot_open_file_picker), Toast.LENGTH_SHORT).show()
                                return false
                            }
                            return true
                        }

                        // Geolocation support
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            if (origin == null || callback == null) return
                            if (ContextCompat.checkSelfPermission(
                                    ctx,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                pendingGeoCallback = Pair(origin, callback)
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )

                            } else {
                                callback.invoke(origin, true, false)
                            }
                        }

                        // Fullscreen video support
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customViewCallback = null
                        }
                    }

                    loadUrl(config.getFormattedUrl())
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Fullscreen View (for web videos)
        customView?.let { cView ->
            AndroidView(
                factory = { cView },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        // Top edge loading progress bar
        if (isLoading && loadingProgress < 1f) {
            LinearProgressIndicator(
                progress = { loadingProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}
