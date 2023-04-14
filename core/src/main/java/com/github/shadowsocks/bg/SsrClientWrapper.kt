package com.github.shadowsocks.bg

object SsrClientWrapper {
    init {
        System.loadLibrary("ssr-client")
    }

    @JvmStatic
    external fun runSsrClient(cmd: ArrayList<String>): Int

    @JvmStatic
    external fun stopSsrClient(): Int
}
