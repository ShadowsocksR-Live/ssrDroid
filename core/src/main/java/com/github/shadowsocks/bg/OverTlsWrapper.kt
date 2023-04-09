package com.github.shadowsocks.bg

import android.net.VpnService

object OverTlsWrapper {
    init {
        System.loadLibrary("overtls")
    }

    @JvmStatic
    external fun runClient(vpnService: VpnService, cofigPath: String, statPath: String, verbose: Boolean): Int

    @JvmStatic
    external fun stopClient(): Int
}
