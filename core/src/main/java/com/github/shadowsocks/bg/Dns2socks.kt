package com.github.shadowsocks.bg

object Dns2socks {
    init {
        System.loadLibrary("dns2socks")
    }

    @JvmStatic
    external fun start(listen_addr: String?,
                       dns_remote_server: String?,
                       socks5_server: String?,
                       username: String?,
                       password: String?,
                       force_tcp: Boolean,
                       cache_records: Boolean,
                       verbosity: Int,
                       timeout: Int): Int

    @JvmStatic
    external fun stop(): Int
}
