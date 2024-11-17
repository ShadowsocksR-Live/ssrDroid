package com.github.shadowsocks.bg

object Tun2proxy {
    init {
        System.loadLibrary("tun2proxy")
    }

    @JvmStatic
    external fun run(proxyUrl: String, tunFd: Int, closeFdOnDrop: Boolean, tunMtu: Char, verbosity: Int, dnsStrategy: Int): Int

    @JvmStatic
    external fun stop(): Int
}
