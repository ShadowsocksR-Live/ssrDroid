package com.github.shadowsocks.bg

import android.net.VpnService

object OverTlsWrapper {
    init {
        System.loadLibrary("overtls")
    }

    @JvmStatic
    external fun runClient(vpnService: VpnService, configPath: String, statPath: String, verbosity: Int): Int

    @JvmStatic
    external fun stopClient(): Int
}
