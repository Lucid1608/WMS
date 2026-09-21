package com.example.wmai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.wmai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingCameraPermissionRequest: PermissionRequest? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingCameraPermissionRequest
            pendingCameraPermissionRequest = null
            if (request == null) return@registerForActivityResult
            if (granted) request.grant(request.resources) else request.deny()
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpWebView()
        setUpSwipeToRefresh()
        setUpRetryButton()
        setUpBackNavigation()

        binding.webView.loadUrl(AppConfig.START_URL)
    }

    private fun setUpWebView() {
        val webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.displayZoomControls = false

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.errorView.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (WebViewErrorPolicy.shouldShowError(request.isForMainFrame)) {
                    showError()
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (WebViewErrorPolicy.shouldShowError(request.isForMainFrame)) {
                    showError()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.progress = newProgress
                binding.progressBar.visibility =
                    if (newProgress in 1..99) View.VISIBLE else View.GONE
                if (newProgress == 100) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources) {
                    requestCameraPermission(request)
                } else {
                    request.deny()
                }
            }
        }

        webView.setDownloadListener(
            DownloadListener { url, _, _, _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        )
    }

    private fun requestCameraPermission(request: PermissionRequest) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            request.grant(request.resources)
            return
        }
        pendingCameraPermissionRequest = request
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun setUpSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.errorView.visibility = View.GONE
            binding.webView.reload()
        }
    }

    private fun setUpRetryButton() {
        binding.retryButton.setOnClickListener {
            binding.errorView.visibility = View.GONE
            binding.webView.reload()
        }
    }

    private fun setUpBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            when (BackNavigationPolicy.resolve(binding.webView.canGoBack())) {
                BackNavigationPolicy.Action.NAVIGATE_WEBVIEW_BACK -> binding.webView.goBack()
                BackNavigationPolicy.Action.EXIT_ACTIVITY -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun showError() {
        binding.swipeRefresh.isRefreshing = false
        binding.progressBar.visibility = View.GONE
        binding.errorView.visibility = View.VISIBLE
    }
}
