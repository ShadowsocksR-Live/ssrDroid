/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.github.shadowsocks.bg

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.github.shadowsocks.Core
import com.github.shadowsocks.VpnRequestActivity
import com.github.shadowsocks.acl.Acl
import com.github.shadowsocks.core.BuildConfig
import com.github.shadowsocks.core.R
import com.github.shadowsocks.net.DefaultNetworkListener
import com.github.shadowsocks.net.HostsFile
import com.github.shadowsocks.net.Subnet
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.printLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class SsrVpnService : VpnService(), LocalDnsService.Interface {
    companion object {
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_CLIENT = "172.19.0.1"
        private const val PRIVATE_VLAN4_ROUTER = "172.19.0.2"
        private const val PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1"
        private const val PRIVATE_VLAN6_ROUTER = "fdfe:dcba:9876::2"
    }

    inner class NullConnectionException : NullPointerException(), BaseService.ExpectedException {
        override fun getLocalizedMessage() = getString(R.string.reboot_required)
    }

    override val data = BaseService.Data(this)
    override val tag: String get() = "ShadowsocksVpnService"
    override fun createNotification(profileName: String): ServiceNotification =
            ServiceNotification(this, profileName, "service-vpn")

    private var conn: ParcelFileDescriptor? = null
    private var active = false
    private var metered = false

    @Volatile
    private var underlyingNetwork: Network? = null
        set(value) {
            field = value
            if (active && Build.VERSION.SDK_INT >= 22) setUnderlyingNetworks(underlyingNetworks)
        }

    // clearing underlyingNetworks makes Android 9 consider the network to be metered
    private val underlyingNetworks
        get() =
            if (Build.VERSION.SDK_INT == 28 && metered) null else underlyingNetwork?.let { arrayOf(it) }

    override fun onBind(intent: Intent) = when (intent.action) {
        SERVICE_INTERFACE -> super<VpnService>.onBind(intent)
        else -> super<LocalDnsService.Interface>.onBind(intent)
    }

    override fun onRevoke() = stopRunner()

    override fun killProcesses(scope: CoroutineScope) {
        tunThread?.terminate()
        tunThread?.join()
        super.killProcesses(scope)
        active = false
        scope.launch { DefaultNetworkListener.stop(this) }
        conn?.close()
        conn = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DataStore.serviceMode == Key.modeVpn) {
            if (prepare(this) != null) {
                startActivity(Intent(this, VpnRequestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else return super<LocalDnsService.Interface>.onStartCommand(intent, flags, startId)
        }
        stopRunner()
        return Service.START_NOT_STICKY
    }

    override suspend fun preInit() = DefaultNetworkListener.start(this) { underlyingNetwork = it }
    override suspend fun getActiveNetwork() = DefaultNetworkListener.get()
    override suspend fun resolver(host: String) = DnsResolverCompat.resolve(DefaultNetworkListener.get(), host)

    override suspend fun startProcesses(hosts: HostsFile) {
        super.startProcesses(hosts)
        startVpn()
    }

    private suspend fun startVpn() {
        val profile = data.proxy!!.profile
        val builder = Builder()
                .setConfigureIntent(Core.configureIntent(this))
                .setSession(profile.formattedName)
                .setMtu(VPN_MTU)
                .addAddress(PRIVATE_VLAN4_CLIENT, 30)
                .addDnsServer(PRIVATE_VLAN4_ROUTER)

        if (profile.ipv6) builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)

        if (profile.proxyApps) {
            val me = packageName
            profile.individual.split('\n')
                    .filter { it != me }
                    .forEach {
                        try {
                            if (profile.bypass) builder.addDisallowedApplication(it)
                            else builder.addAllowedApplication(it)
                        } catch (ex: PackageManager.NameNotFoundException) {
                            printLog(ex)
                        }
                    }
            if (!profile.bypass) builder.addAllowedApplication(me)
        }

        when (profile.route) {
            Acl.ALL, Acl.BYPASS_CHN, Acl.CUSTOM_RULES -> {
                builder.addRoute("0.0.0.0", 0)
                if (profile.ipv6) builder.addRoute("::", 0)
            }
            else -> {
                resources.getStringArray(R.array.bypass_private_route).forEach {
                    val subnet = Subnet.fromString(it)!!
                    builder.addRoute(subnet.address.hostAddress, subnet.prefixSize)
                }
                builder.addRoute(PRIVATE_VLAN4_ROUTER, 32)
                // https://issuetracker.google.com/issues/149636790
                if (profile.ipv6) builder.addRoute("2000::", 3)
            }
        }

        metered = profile.metered
        active = true   // possible race condition here?
        if (Build.VERSION.SDK_INT >= 22) {
            builder.setUnderlyingNetworks(underlyingNetworks)
            if (Build.VERSION.SDK_INT >= 29) builder.setMetered(metered)
        }

        val conn = builder.establish() ?: throw NullConnectionException()
        this.conn = conn

        val cmd = arrayListOf(
                File(applicationInfo.nativeLibraryDir, Executable.TUN2SOCKS).absolutePath,
                "--netif-ipaddr", PRIVATE_VLAN4_ROUTER,
                "--socks-server-addr", "${DataStore.listenAddress}:${DataStore.portProxy}",
                "--tunmtu", VPN_MTU.toString(),
                "--sock-path", "sock_path",
                "--dnsgw", "127.0.0.1:${DataStore.portLocalDns}",
                "--loglevel", "warning"
        )
        if (profile.ipv6) {
            cmd += "--netif-ip6addr"
            cmd += PRIVATE_VLAN6_ROUTER
        }
        cmd += "--enable-udprelay"

        cmd += "--proxy"
        cmd += "socks5://${DataStore.listenAddress}:${DataStore.portProxy}"

        cmd += "--tunfd"
        cmd += conn.getFd().toString()

        if (BuildConfig.DEBUG) cmd += "--verbose"

        tunThread = Tun2proxyThread(cmd)
        tunThread?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        data.binder.close()
    }

    private var tunThread: Tun2proxyThread? = null

    internal class Tun2proxyThread(cmd: ArrayList<String>?) : Thread() {
        private var cmd: ArrayList<String>? = null
        init {
            this.cmd = cmd
        }

        override fun run() {
            super.run()
            if (cmd != null) {
                Tun2proxy.run(proxyUrl(), tunFd(), tunMtu(), verbose())
            }
        }

        fun terminate() {
            Tun2proxy.stop()
        }

        fun proxyUrl(): String {
            val index = cmd?.indexOfFirst { it == "--proxy" }
            return cmd?.get(index?.plus(1) ?: -1).toString()
        }

        fun tunFd(): Int {
            val index = cmd?.indexOfFirst { it == "--tunfd" }
            val fd = cmd?.get(index?.plus(1) ?: -1).toString()
            return fd.toInt()
        }

        fun tunMtu(): Int {
            val index = cmd?.indexOfFirst { it == "--tunmtu" }
            val mtu = cmd?.get(index?.plus(1) ?: -1).toString()
            return mtu.toInt()
        }

        fun verbose(): Boolean {
            val v = cmd?.indexOfFirst { it == "--verbose" } ?: -1
            return v >= 0
        }
    }
}
