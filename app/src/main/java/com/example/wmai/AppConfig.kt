package com.example.wmai

/** Central place for the NetSuite mobile emulator's entry point. */
object AppConfig {
    /**
     * NetSuite's "Mobile Login Page URL" for this mobile role (Suitelet script 896, deploy 1,
     * account 5150631), including its `ns-at` access token. This is NOT a per-session cookie
     * token: it's a persistent, bookmarkable URL NetSuite issues for exactly this purpose.
     * Loading the Suitelet without it fails hard with NetSuite's own "Invalid page parameter.
     * Unable to view page." notice, rather than falling back to a login page — confirmed
     * on-device, so the full URL must be used as-is.
     */
    const val START_URL =
        "https://5150631.extforms.netsuite.com/app/site/hosting/scriptlet.nl?script=896&deploy=1&compid=5150631&ns-at=AAEJ7tMQNuTcbiL5Sjq4-A6iX43rOrLo6FQcXcVuD9nQNgqUsxI"
}
