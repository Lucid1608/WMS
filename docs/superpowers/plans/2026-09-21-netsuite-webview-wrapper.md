# NetSuite Mobile Emulator WebView Wrapper — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-activity Android app that opens directly into the company's NetSuite
mobile emulator (Suitelet script 896) in a WebView, targeting Android 8.0+ (API 26).

**Architecture:** `MainActivity` hosts a `WebView` wrapped in a `SwipeRefreshLayout`, with a
progress bar and a full-screen error/retry view. Two pieces of decision logic are extracted as
pure, unit-testable objects (`BackNavigationPolicy`, `WebViewErrorPolicy`) so the Android
framework glue in `MainActivity` stays thin.

**Tech Stack:** Kotlin, AndroidX (`appcompat`, `core-ktx`, `material`, `swiperefreshlayout`),
`android.webkit.WebView`, ViewBinding, JUnit4 for local unit tests.

**Environment note:** The `java` on `PATH` in this shell is Java 8, which is too old for
AGP 9.4.1 / Gradle 9.6.0. Android Studio's bundled JBR (Java 25) is at
`C:\Program Files\Android\Android Studio\jbr`. Every Gradle command in this plan must be run
with `JAVA_HOME` pointed there, e.g.:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew <task>
```

---

### Task 1: Add Kotlin support and the SwipeRefreshLayout dependency

The scaffold currently applies only the Android application plugin — no Kotlin plugin — even
though `.kt` files exist under `src/test` and `src/androidTest`. Without the Kotlin Android
plugin, no `.kt` file in `src/main` would be compiled. This task fixes that, bumps `minSdk` to
26, and adds the `SwipeRefreshLayout` dependency and ViewBinding.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add Kotlin and SwipeRefreshLayout entries to the version catalog**

Edit `gradle/libs.versions.toml` to match exactly:

```toml
[versions]
agp = "9.4.1"
coreKtx = "1.10.1"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
kotlin = "2.4.20"
swiperefreshlayout = "1.2.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-swiperefreshlayout = { group = "androidx.swiperefreshlayout", name = "swiperefreshlayout", version.ref = "swiperefreshlayout" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 2: Apply the Kotlin plugin, bump minSdk, enable ViewBinding, add the dependency**

Edit `app/build.gradle.kts` to match exactly:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.wmai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.wmai"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

- [ ] **Step 3: Verify the project still configures correctly**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew help`
Expected: `BUILD SUCCESSFUL`. If it fails, fix the reported error in the two files above before
continuing — do not proceed to Task 2 with a broken configuration.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "Add Kotlin plugin, bump minSdk to 26, add SwipeRefreshLayout dependency"
```

---

### Task 2: BackNavigationPolicy (pure logic, TDD)

Decides what the Android back button should do: navigate the WebView back if it has history,
otherwise fall through to exiting the activity.

**Files:**
- Create: `app/src/main/java/com/example/wmai/BackNavigationPolicy.kt`
- Test: `app/src/test/java/com/example/wmai/BackNavigationPolicyTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/wmai/BackNavigationPolicyTest.kt`:

```kotlin
package com.example.wmai

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationPolicyTest {

    @Test
    fun `navigates webview back when webview has history`() {
        val action = BackNavigationPolicy.resolve(webViewCanGoBack = true)
        assertEquals(BackNavigationPolicy.Action.NAVIGATE_WEBVIEW_BACK, action)
    }

    @Test
    fun `exits activity when webview has no history`() {
        val action = BackNavigationPolicy.resolve(webViewCanGoBack = false)
        assertEquals(BackNavigationPolicy.Action.EXIT_ACTIVITY, action)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.BackNavigationPolicyTest"`
Expected: FAIL (compilation error — `BackNavigationPolicy` does not exist yet).

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/wmai/BackNavigationPolicy.kt`:

```kotlin
package com.example.wmai

/**
 * Decides what the Android back button should do based on whether the WebView
 * has in-page navigation history to go back to.
 */
object BackNavigationPolicy {

    enum class Action { NAVIGATE_WEBVIEW_BACK, EXIT_ACTIVITY }

    fun resolve(webViewCanGoBack: Boolean): Action =
        if (webViewCanGoBack) Action.NAVIGATE_WEBVIEW_BACK else Action.EXIT_ACTIVITY
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.BackNavigationPolicyTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/wmai/BackNavigationPolicy.kt app/src/test/java/com/example/wmai/BackNavigationPolicyTest.kt
git commit -m "Add BackNavigationPolicy with unit tests"
```

---

### Task 3: WebViewErrorPolicy (pure logic, TDD)

Decides whether a WebView load failure should surface the full-screen error/retry view.
Subresource failures (a missing icon, a blocked tracking pixel, etc.) are ignored so a single
broken subresource doesn't hide a page that otherwise loaded fine.

**Files:**
- Create: `app/src/main/java/com/example/wmai/WebViewErrorPolicy.kt`
- Test: `app/src/test/java/com/example/wmai/WebViewErrorPolicyTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/wmai/WebViewErrorPolicyTest.kt`:

```kotlin
package com.example.wmai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewErrorPolicyTest {

    @Test
    fun `shows error when main frame fails to load`() {
        assertTrue(WebViewErrorPolicy.shouldShowError(isForMainFrame = true))
    }

    @Test
    fun `ignores subresource failures`() {
        assertFalse(WebViewErrorPolicy.shouldShowError(isForMainFrame = false))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.WebViewErrorPolicyTest"`
Expected: FAIL (compilation error — `WebViewErrorPolicy` does not exist yet).

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/wmai/WebViewErrorPolicy.kt`:

```kotlin
package com.example.wmai

/**
 * Decides whether a WebView load failure should surface the full-screen error/retry view.
 * Subresource failures (a missing icon, a blocked tracking pixel, etc.) are ignored so a
 * single broken subresource doesn't hide a page that otherwise loaded fine.
 */
object WebViewErrorPolicy {

    fun shouldShowError(isForMainFrame: Boolean): Boolean = isForMainFrame
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.WebViewErrorPolicyTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/wmai/WebViewErrorPolicy.kt app/src/test/java/com/example/wmai/WebViewErrorPolicyTest.kt
git commit -m "Add WebViewErrorPolicy with unit tests"
```

---

### Task 4: AppConfig (pure data, TDD)

Holds the NetSuite Suitelet start URL, deliberately excluding the session-bound `ns-at` token.

**Files:**
- Create: `app/src/main/java/com/example/wmai/AppConfig.kt`
- Test: `app/src/test/java/com/example/wmai/AppConfigTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/wmai/AppConfigTest.kt`:

```kotlin
package com.example.wmai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun `start url does not embed a session token`() {
        assertFalse(AppConfig.START_URL.contains("ns-at"))
    }

    @Test
    fun `start url uses https`() {
        assertTrue(AppConfig.START_URL.startsWith("https://"))
    }

    @Test
    fun `start url targets the expected suitelet`() {
        assertTrue(AppConfig.START_URL.contains("script=896"))
        assertTrue(AppConfig.START_URL.contains("compid=5150631"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.AppConfigTest"`
Expected: FAIL (compilation error — `AppConfig` does not exist yet).

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/wmai/AppConfig.kt`:

```kotlin
package com.example.wmai

/** Central place for the NetSuite mobile emulator's entry point. */
object AppConfig {
    /**
     * Base URL for the NetSuite Suitelet (script 896, deploy 1, account 5150631) that
     * renders the mobile emulator. Deliberately excludes the `ns-at` session token that
     * NetSuite appends to shared links — that token expires, so landing here lets
     * NetSuite's own login page take over whenever there's no valid session.
     */
    const val START_URL =
        "https://5150631.extforms.netsuite.com/app/site/hosting/scriptlet.nl?script=896&deploy=1&compid=5150631"
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest --tests "com.example.wmai.AppConfigTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/wmai/AppConfig.kt app/src/test/java/com/example/wmai/AppConfigTest.kt
git commit -m "Add AppConfig with start URL and unit tests"
```

---

### Task 5: Strings and layout

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Update strings.xml**

Edit `app/src/main/res/values/strings.xml` to match exactly:

```xml
<resources>
    <string name="app_name">WMA</string>
    <string name="error_message">Couldn\'t load this page. Check your connection and try again.</string>
    <string name="retry">Retry</string>
</resources>
```

- [ ] **Step 2: Create the activity layout**

Create `app/src/main/res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <WebView
            android:id="@+id/webView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <ProgressBar
        android:id="@+id/progressBar"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="top"
        android:max="100"
        android:visibility="gone" />

    <LinearLayout
        android:id="@+id/errorView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="?android:attr/colorBackground"
        android:gravity="center"
        android:orientation="vertical"
        android:padding="24dp"
        android:visibility="gone">

        <TextView
            android:id="@+id/errorMessage"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            android:gravity="center"
            android:text="@string/error_message"
            android:textAppearance="?android:attr/textAppearanceMedium" />

        <Button
            android:id="@+id/retryButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/retry" />

    </LinearLayout>

</FrameLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/layout/activity_main.xml
git commit -m "Add activity_main layout and strings"
```

---

### Task 6: AndroidManifest permissions and activity registration

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Update the manifest**

Edit `app/src/main/AndroidManifest.xml` to match exactly:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.WMAI">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "Register MainActivity and add INTERNET/CAMERA permissions"
```

---

### Task 7: MainActivity

Wires the WebView to the two policy objects, handles cookie persistence, progress, pull-to-
refresh, retry, back navigation, downloads, and camera permission requests from the page.

**Files:**
- Create: `app/src/main/java/com/example/wmai/MainActivity.kt`

- [ ] **Step 1: Write the implementation**

Create `app/src/main/java/com/example/wmai/MainActivity.kt`:

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/example/wmai/MainActivity.kt
git commit -m "Implement MainActivity WebView wrapper"
```

---

### Task 8: Full build verification

**Files:** none (verification only)

- [ ] **Step 1: Run all local unit tests**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests (including the pre-existing `ExampleUnitTest`) pass.

- [ ] **Step 2: Build the debug APK**

Run: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, producing `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Fix any errors surfaced by Steps 1–2**

If either command fails, read the actual compiler/test output, fix the responsible file from
an earlier task, and re-run until both commands succeed. Do not consider the plan complete
until both are green.

- [ ] **Step 4: Commit any fixes made in Step 3**

```bash
git add -A
git commit -m "Fix build issues found during verification"
```

(Skip this step if Steps 1–2 passed with no changes needed.)

---

## Manual on-device verification (cannot be done from this environment)

Note in the final report to the user, not a task to execute here:
- Confirm the Zebra handheld's built-in scanner (DataWedge keystroke wedge) still types into
  whatever NetSuite field has focus inside the WebView.
- Confirm the NetSuite login session persists across an app relaunch (cookie persistence).
- Confirm the app installs and runs on a real Android 8.0+ device, not just the Gradle build.
