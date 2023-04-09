/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2019 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2019 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
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

import android.content.Context
import com.github.shadowsocks.acl.Acl
import com.github.shadowsocks.acl.AclSyncer
import com.github.shadowsocks.core.BuildConfig
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.net.HostsFile
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.parseNumericAddress
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.UnknownHostException

/**
 * This class sets up environment for ss-local.
 */
class ProxyInstance(val profile: Profile, private val route: String = profile.route) {
    private var configFile: File? = null
    var trafficMonitor: TrafficMonitor? = null
    private val host = profile.host

    private var myThread: SsrClientThread? = null

    suspend fun init(service: BaseService.Interface, hosts: HostsFile) {

        // it's hard to resolve DNS on a specific interface so we'll do it here
        if (profile.host.parseNumericAddress() == null) {
            profile.host = hosts.resolve(profile.host).run {
                if (isEmpty()) try {
                    service.resolver(profile.host).firstOrNull()
                } catch (_: IOException) {
                    null
                } else {
                    val network = service.getActiveNetwork() ?: throw UnknownHostException()
                    val hasIpv4 = DnsResolverCompat.haveIpv4(network)
                    val hasIpv6 = DnsResolverCompat.haveIpv6(network)
                    firstOrNull {
                        when (it) {
                            is Inet4Address -> hasIpv4
                            is Inet6Address -> hasIpv6
                            else -> error(it)
                        }
                    }
                }
            }?.hostAddress ?: throw UnknownHostException()
        }
    }

    /**
     * Sensitive shadowsocks configuration file requires extra protection. It may be stored in encrypted storage or
     * device storage, depending on which is currently available.
     */
    fun start(
        service: BaseService.Interface, stat: File, configFile: File, extraFlag: String? = null
    ) {
        trafficMonitor = TrafficMonitor(stat)

        this.configFile = configFile
        val config = profile.toJson()

        configFile.writeText(config.toString())

        val cmd = arrayListOf(
            File(
                (service as Context).applicationInfo.nativeLibraryDir, Executable.SSR_CLIENT
            ).absolutePath, "-V", "-S", stat.absolutePath, "-c", configFile.absolutePath
        )
        if (extraFlag != null) cmd.add(extraFlag)

        if (route != Acl.ALL) {
            cmd += "--acl"
            cmd += Acl.getFile(route).absolutePath
        }

        if (DataStore.tcpFastOpen) cmd += "--fast-open"
        if (BuildConfig.DEBUG) cmd += "-v"

        myThread = SsrClientThread(service as VpnService, profile.isOverTLS(), cmd)
        myThread?.start()
    }

    fun scheduleUpdate() {
        if (route !in arrayOf(Acl.ALL, Acl.CUSTOM_RULES)) AclSyncer.schedule(route)
    }

    fun shutdown(scope: CoroutineScope) {
        myThread?.terminate()

        trafficMonitor?.apply {
            thread.shutdown(scope)
            persistStats(profile.id)    // Make sure update total traffic when stopping the runner
        }
        trafficMonitor = null
        configFile?.delete()    // remove old config possibly in device storage
        configFile = null
    }

    internal class SsrClientThread(
        vpnService: VpnService, isOverTls: Boolean, cmd: ArrayList<String>?
    ) : Thread() {
        private var cmd: ArrayList<String>? = null
        private var vpnService: VpnService? = null
        private var isOverTls: Boolean = false

        init {
            this.cmd = cmd
            this.vpnService = vpnService
            this.isOverTls = isOverTls
        }

        fun configPath(): String {
            val index = cmd?.indexOfFirst { it == "-c" }
            return cmd?.get(index?.plus(1) ?: -1).toString()
        }

        fun statPath(): String {
            val v = cmd?.indexOfFirst { it == "-V" } ?: -1
            val index = cmd?.indexOf("-S")
            if (v >= 0) {
                return cmd?.get(index?.plus(1) ?: -1).toString()
            } else {
                return ""
            }
        }

        fun verbose(): Boolean {
            val v = cmd?.indexOfFirst { it == "-v" } ?: -1
            return v >= 0
        }

        override fun run() {
            super.run()
            if (isOverTls) {
                vpnService?.let { OverTlsWrapper.runClient(it, configPath(), statPath(), verbose()) }
            } else {
                cmd?.let { SsrClientWrapper.runSsrClient(it) }
            }
        }

        fun terminate() {
            if (isOverTls) {
                OverTlsWrapper.stopClient()
            } else {
                SsrClientWrapper.stopSsrClient()
            }
        }
    }
}
