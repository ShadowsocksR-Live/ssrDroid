package com.github.shadowsocks.bg

object Tun2proxy {
    init {
        System.loadLibrary("tun2proxy")
    }

    @JvmStatic
    external fun run(proxyUrl: String, tunFd: Int, tunMtu: Int, verbose: Boolean, dnsOverTcp: Boolean): Int

    @JvmStatic
    external fun stop(): Int
}
