# NetSuite Mobile Emulator Wrapper App — Design

**Date:** 2026-09-21
**Status:** Approved

## Purpose

Give warehouse staff a native Android app (installed on Zebra handhelds, Android 8.0+) that
opens directly into the company's NetSuite mobile emulator (a Suitelet, script id 896,
deploy 1, account 5150631) instead of requiring them to navigate there in a general browser.

Out of scope for this iteration (explicitly deferred by the user, to revisit later):
- Fixing "Ship Central" screen behavior (button order, complete-packing making orders
  disappear, shop-for-rates vs. ship-later placement) — this logic lives server-side in the
  NetSuite Suitelet/client scripts behind script 896, not in this Android project.
- Scan-to-open-order regression — same reason; the Zebra handheld's built-in scanner acts as
  a keyboard-wedge input, so this is a NetSuite-side (or DataWedge config) fix, not something
  the wrapper app controls.
- Persistent sign-in as a dedicated feature — addressed only as a side effect of standard
  WebView cookie persistence (see below), not a bespoke auth flow.

## Architecture

Single-activity Android app. `MainActivity` hosts one full-screen `WebView` pointed at the
NetSuite Suitelet base URL. No navigation graph, no additional screens.

## Components

- **MainActivity + WebView** — loads the start URL on launch.
  - `WebViewClient` handles in-page navigation and reports load errors.
  - `WebChromeClient` reports load progress (for a loading indicator) and handles JS
    permission requests (e.g. camera, if the page ever calls `getUserMedia`).
- **SwipeRefreshLayout** wraps the WebView for pull-to-refresh, useful given warehouse Wi-Fi
  can be unreliable.
- **Error/offline view** — shown when a page fails to load or there's no network: a short
  message plus a Retry button. Hidden otherwise.
- **Back button** — navigates WebView history (`goBack()`) if there is history; otherwise
  falls through to normal Android back behavior (exits the app).
- **Download safety net** — a `DownloadListener` that hands any download off to the system
  browser/download manager, so a download attempt doesn't silently fail. No new permission
  required.

## Configuration

- `minSdk` raised from 24 to 26 (Android 8.0), matching the stated requirement. `targetSdk`
  and `compileSdk` remain 37.
- WebView settings: JavaScript enabled, DOM storage enabled, cookies enabled and set to
  persist across app restarts via `CookieManager` — this is standard WebView behavior and is
  what lets a NetSuite session survive an app relaunch, addressing the "don't want to sign in
  every time" feedback without any bespoke auth code.
- Permissions: `INTERNET` (required); `CAMERA` declared but only requested at runtime if the
  page actually invokes `getUserMedia` (future-proofing; the Zebra hardware scanner does not
  need this).
- App label: "WMA" (placeholder), default launcher icon (placeholder, swappable later).
- Start URL: the Suitelet base URL without the session-specific `ns-at` token
  (`https://5150631.extforms.netsuite.com/app/site/hosting/scriptlet.nl?script=896&deploy=1&compid=5150631`),
  since the token in the URL the user supplied is session-bound and will expire. Landing on
  the base URL lets NetSuite's own login page take over when there's no valid session.

## Explicitly not included

File upload UI, native DataWedge/scanner integration, kiosk/screen-pinning lock, and a
settings screen for changing the target URL. None were requested and each adds complexity
without a stated need.

## Testing

Verified via Gradle build (`assembleDebug`). Real-device behavior — scanner focus/DataWedge
interaction, actual NetSuite session handling on a Zebra handheld — cannot be verified from
this environment and should be checked on-device.
