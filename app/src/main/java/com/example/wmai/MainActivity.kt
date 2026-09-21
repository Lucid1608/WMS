package com.example.wmai

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wmai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingCameraPermissionRequest: PermissionRequest? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingCameraPermissionRequest
            pendingCameraPermissionRequest = null
            if (request == null) return@registerForActivityResult
            if (granted) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request.deny()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpEdgeToEdgeInsets()
        setUpWebView()
        setUpSwipeToRefresh()
        setUpRetryButton()
        setUpBackNavigation()

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            binding.webView.loadUrl(AppConfig.START_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            webViewClient = WebViewClient()
            webChromeClient = null
            // WebView must be detached from the view hierarchy before destroy() is called.
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }

    private fun setUpEdgeToEdgeInsets() {
        // targetSdk 37 forces edge-to-edge layout with no opt-out: content draws behind the
        // status bar and navigation bar by default, and their color is no longer themeable.
        // Pad the root view by the system bars (and the keyboard, when shown) so our content
        // never sits underneath them.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
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

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Force any session cookie NetSuite just set to disk now, rather than relying
                // on CookieManager's periodic auto-flush, so a killed/force-closed app doesn't
                // lose the session and force a fresh sign-in next launch.
                CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (WebViewErrorPolicy.shouldShowError(request.isForMainFrame, error.description?.toString())) {
                    showError()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (LinkSchemePolicy.isWebScheme(request.url.scheme)) {
                    return false
                }
                // tel:, mailto:, geo:, market:, etc. — WebView can't navigate these itself.
                // Hand off to the system; either way, don't let WebView also try and fail,
                // which would otherwise trigger the error view.
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
                }
                return true
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
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.download_failed, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun requestCameraPermission(request: PermissionRequest) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
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
