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
