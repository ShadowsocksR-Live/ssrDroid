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

package com.github.shadowsocks.utils

import com.github.shadowsocks.Core.app

object Key {
    /**
     * Public config that doesn't need to be kept secret.
     */
    const val DB_PUBLIC = "config.db"
    const val DB_PROFILE = "profile.db"

    const val id = "profileId"
    const val name = "profileName"
    const val group = "groupName"

    const val individual = "Proxyed"

    const val serviceMode = "serviceMode"
    const val modeProxy = "proxy"
    const val modeVpn = "vpn"
    const val shareOverLan = "shareOverLan"
    const val portProxy = "portProxy"
    const val portLocalDns = "portLocalDns"
    const val portTransproxy = "portTransproxy"
    const val useTun2proxy = "useTun2proxy"
    const val useOverTLS = "useOverTLS"

    const val route = "route"

    const val persistAcrossReboot = "isAutoConnect"
    const val directBootAware = "directBootAware"

    const val proxyApps = "isProxyApps"
    const val bypass = "isBypassApps"
    const val udpdns = "isUdpDns"
    const val ipv6 = "isIpv6"
    const val metered = "metered"

    const val host = "proxy"
    const val password = "sitekey"
    const val method = "encMethod"
    const val protocol = "ssrProtocol"
    const val protocol_param = "ssrProtocolParam"
    const val obfs = "ssrObfs"
    const val obfs_param = "ssrObfsParam"
    const val over_tls_enable = "over_tls_enable"
    const val over_tls_server_domain = "over_tls_server_domain"
    const val over_tls_path = "over_tls_path"
    const val remotePort = "remotePortNum"
    const val remoteDns = "remoteDns"

    const val plugin = "plugin"
    const val pluginConfigure = "plugin.configure"
    const val udpFallback = "udpFallback"

    const val dirty = "profileDirty"

    const val tfo = "tcp_fastopen"
    const val hosts = "hosts"
    const val assetUpdateTime = "assetUpdateTime"

    // TV specific values
    const val controlStats = "control.stats"
    const val controlImport = "control.import"
    const val controlExport = "control.export"

    const val about = "about"
    const val aclUrl = "acl_url"
    const val socksHost = "socksHost"
    const val socksPort = "socksPort"
    const val socksUser = "socksUser"
    const val socksPswd = "socksPswd"

}

object Action {
    val SERVICE = app.packageName + ".SERVICE"
    val CLOSE = app.packageName + ".CLOSE"
    val RELOAD = app.packageName + ".RELOAD"
    val ABORT = app.packageName + ".ABORT"

    val EXTRA_PROFILE_ID = app.packageName + "EXTRA_PROFILE_ID"
}
