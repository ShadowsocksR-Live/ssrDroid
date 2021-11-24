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

package com.github.shadowsocks.database

import android.annotation.TargetApi
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import android.util.LongSparseArray
import androidx.core.net.toUri
import androidx.room.*
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.parsePort
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException
import java.util.*

@Entity
@Parcelize
data class Profile(
        @PrimaryKey(autoGenerate = true)
        var id: Long = 0,
        var name: String = "Pending edit",
        var host: String = "",
        var remotePort: Int = 8388,
        var password: String = "",
        var protocol: String = "origin",
        var protocol_param: String = "",
        var obfs: String = "plain",
        var obfs_param: String = "",
        var method: String = "aes-256-cfb",

        var route: String = "all",
        var remoteDns: String = "8.8.8.8:53",
        var proxyApps: Boolean = false,
        var bypass: Boolean = false,
        var udpdns: Boolean = false,
        var url_group: String = "",
        var ipv6: Boolean = false,
        @TargetApi(28)
        var metered: Boolean = false,
        var individual: String = "",
        var plugin: String? = null,
        var udpFallback: Long? = null,

        // managed fields
        var subscription: SubscriptionStatus = SubscriptionStatus.UserConfigured,
        var tx: Long = 0,
        var rx: Long = 0,
        var elapsed: Long = 0,
        var userOrder: Long = 0,

        @Ignore // not persisted in db, only used by direct boot
        var dirty: Boolean = false
) : Parcelable, Serializable {
    enum class SubscriptionStatus(val persistedValue: Int) {
        UserConfigured(0),
        Active(1),
        /**
         * This profile is no longer present in subscriptions.
         */
        Obsolete(2),
        ;

        companion object {
            @JvmStatic
            @TypeConverter
            fun of(value: Int) = values().single { it.persistedValue == value }
            @JvmStatic
            @TypeConverter
            fun toInt(status: SubscriptionStatus) = status.persistedValue
        }
    }

    companion object {
        private const val TAG = "ShadowParser"
        private const val serialVersionUID = 1L
        private val pattern =
                """(?i)ss://[-a-zA-Z0-9+&@#/%?=.~*'()|!:,;_\[\]]*[-a-zA-Z0-9+&@#/%=.~*'()|\[\]]""".toRegex()
        private val userInfoPattern = "^(.+?):(.*)$".toRegex()
        private val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)$".toRegex()

        private val pattern_ssr = "(?i)ssr://([A-Za-z0-9_=-]+)".toRegex()
        private val decodedPattern_ssr = "(?i)^((.+):(\\d+?):(.*):(.+):(.*):(.+)/(.*))".toRegex()
        private val decodedPattern_ssr_obfsparam = "(?i)(.*)[?&]obfsparam=([A-Za-z0-9_=-]*)(.*)".toRegex()
        private val decodedPattern_ssr_remarks = "(?i)(.*)[?&]remarks=([A-Za-z0-9_=-]*)(.*)".toRegex()
        private val decodedPattern_ssr_protocolparam = "(?i)(.*)[?&]protoparam=([A-Za-z0-9_=-]*)(.*)".toRegex()
        private val decodedPattern_ssr_groupparam = "(?i)(.*)[?&]group=([A-Za-z0-9_=-]*)(.*)".toRegex()

        private fun base64Decode(data: String) = String(Base64.decode(data.replace("=", ""), Base64.URL_SAFE), Charsets.UTF_8)

        fun findAllSSRUrls(data: CharSequence?, feature: Profile? = null) = pattern_ssr.findAll(data
                ?: "").map {
            val uri = base64Decode(it.groupValues[1])
            try {
                val match = decodedPattern_ssr.matchEntire(uri)
                if (match != null) {
                    val profile = Profile()
                    feature?.copyFeatureSettingsTo(profile)
                    profile.host = match.groupValues[2].lowercase(Locale.ENGLISH)
                    profile.remotePort = match.groupValues[3].toInt()
                    profile.protocol = match.groupValues[4].lowercase(Locale.ENGLISH)
                    profile.method = match.groupValues[5].lowercase(Locale.ENGLISH)
                    profile.obfs = match.groupValues[6].lowercase(Locale.ENGLISH)
                    profile.password = base64Decode(match.groupValues[7])

                    val match1 = decodedPattern_ssr_obfsparam.matchEntire(match.groupValues[8])
                    if (match1 != null) profile.obfs_param = base64Decode(match1.groupValues[2])

                    val match2 = decodedPattern_ssr_protocolparam.matchEntire(match.groupValues[8])
                    if (match2 != null) profile.protocol_param = base64Decode(match2.groupValues[2])

                    val match3 = decodedPattern_ssr_remarks.matchEntire(match.groupValues[8])
                    if (match3 != null) profile.name = base64Decode(match3.groupValues[2])

                    val match4 = decodedPattern_ssr_groupparam.matchEntire(match.groupValues[8])
                    if (match4 != null) profile.url_group = base64Decode(match4.groupValues[2])

                    profile
                } else {
                    null
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid SSR URI: ${it.value}")
                null
            }
        }.filterNotNull().toMutableSet()

        private fun findAllSSUrls(data: CharSequence?, feature: Profile? = null) = pattern.findAll(data
                ?: "").map {
            val uri = it.value.toUri()
            try {
                if (uri.userInfo == null) {
                    val match = legacyPattern.matchEntire(String(Base64.decode(uri.host, Base64.NO_PADDING)))
                    if (match != null) {
                        val profile = Profile()
                        feature?.copyFeatureSettingsTo(profile)
                        profile.method = match.groupValues[1].lowercase(Locale.ENGLISH)
                        profile.password = match.groupValues[2]
                        profile.host = match.groupValues[3]
                        profile.remotePort = match.groupValues[4].toInt()
                        profile.plugin = uri.getQueryParameter(Key.plugin)
                        profile.name = uri.fragment.toString()
                        profile
                    } else {
                        Log.e(TAG, "Unrecognized URI: ${it.value}")
                        null
                    }
                } else {
                    val match = userInfoPattern.matchEntire(String(Base64.decode(uri.userInfo,
                            Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)))
                    if (match != null) {
                        val profile = Profile()
                        feature?.copyFeatureSettingsTo(profile)
                        profile.method = match.groupValues[1]
                        profile.password = match.groupValues[2]
                        // bug in Android: https://code.google.com/p/android/issues/detail?id=192855
                        try {
                            val javaURI = URI(it.value)
                            profile.host = javaURI.host ?: ""
                            if (profile.host.firstOrNull() == '[' && profile.host.lastOrNull() == ']') {
                                profile.host = profile.host.substring(1, profile.host.length - 1)
                            }
                            profile.remotePort = javaURI.port
                            profile.plugin = uri.getQueryParameter(Key.plugin)
                            profile.name = uri.fragment ?: ""
                            profile
                        } catch (e: URISyntaxException) {
                            Log.e(TAG, "Invalid URI: ${it.value}")
                            null
                        }
                    } else {
                        Log.e(TAG, "Unknown user info: ${it.value}")
                        null
                    }
                }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid base64 detected: ${it.value}")
                null
            }
        }.filterNotNull().toMutableSet()

        fun findAllUrls(data: CharSequence?, feature: Profile? = null) =
                findAllSSRUrls(data, feature).plus(findAllSSUrls(data, feature))

        private class JsonParser(private val feature: Profile? = null) : ArrayList<Profile>() {
            val fallbackMap = mutableMapOf<Profile, Profile>()

            private val JsonElement?.optString get() = (this as? JsonPrimitive)?.asString
            private val JsonElement?.optBoolean
                get() = // asBoolean attempts to cast everything to boolean
                    (this as? JsonPrimitive)?.run { if (isBoolean) asBoolean else null }
            private val JsonElement?.optInt
                get() = try {
                    (this as? JsonPrimitive)?.asInt
                } catch (_: NumberFormatException) {
                    null
                }

            private fun tryParse(json: JsonObject, fallback: Boolean = false): Profile? {
                val host = json["server"].optString
                if (host.isNullOrEmpty()) return null
                val remotePort = json["server_port"]?.optInt
                if (remotePort == null || remotePort <= 0) return null
                val password = json["password"].optString
                if (password.isNullOrEmpty()) return null
                val protocol = json["protocol"].optString
                if (protocol.isNullOrEmpty()) return null
                val protocolParam = json["protocol_param"].optString ?: return null
                val obfs = json["obfs"].optString
                if (obfs.isNullOrEmpty()) return null
                val obfsParam = json["obfs_param"].optString ?: return null
                val method = json["method"].optString
                if (method.isNullOrEmpty()) return null
                return Profile().also {
                    it.host = host
                    it.remotePort = remotePort
                    it.password = password
                    it.method = method
                    it.protocol = protocol
                    it.protocol_param = protocolParam
                    it.obfs = obfs
                    it.obfs_param = obfsParam
                }.apply {
                    feature?.copyFeatureSettingsTo(this)
                    name = json["remarks"].optString.toString()
                    route = json["route"].optString ?: route
                    if (fallback) return@apply
                    remoteDns = json["remote_dns"].optString ?: remoteDns
                    ipv6 = json["ipv6"].optBoolean ?: ipv6
                    metered = json["metered"].optBoolean ?: metered
                    (json["proxy_apps"] as? JsonObject)?.also {
                        proxyApps = it["enabled"].optBoolean ?: proxyApps
                        bypass = it["bypass"].optBoolean ?: bypass
                        individual = (it["android_list"] as? JsonArray)?.asIterable()?.mapNotNull { it.optString }
                                ?.joinToString("\n") ?: individual
                    }
                    udpdns = json["udpdns"].optBoolean ?: udpdns
                    (json["udp_fallback"] as? JsonObject)?.let { tryParse(it, true) }?.also { fallbackMap[this] = it }
                }
            }

            fun process(json: JsonElement?) {
                when (json) {
                    is JsonObject -> {
                        val profile = tryParse(json)
                        if (profile != null) add(profile) else for ((_, value) in json.entrySet()) process(value)
                    }
                    is JsonArray -> json.asIterable().forEach(this::process)
                    // ignore other types
                }
            }

            fun finalize(create: (Profile) -> Profile) {
                val profiles = ProfileManager.getAllProfiles() ?: emptyList()
                for ((profile, fallback) in fallbackMap) {
                    val match = profiles.firstOrNull {
                        fallback.host == it.host && fallback.remotePort == it.remotePort &&
                                fallback.password == it.password && fallback.method == it.method &&
                                fallback.protocol == it.protocol && fallback.protocol_param == it.protocol_param &&
                                fallback.obfs == it.obfs && fallback.obfs_param == it.obfs_param &&
                                it.plugin.isNullOrEmpty()
                    }
                    profile.udpFallback = (match ?: create(fallback)).id
                    ProfileManager.updateProfile(profile)
                }
            }
        }

        fun parseJson(json: JsonElement, feature: Profile? = null, create: (Profile) -> Profile) {
            JsonParser(feature).run {
                process(json)
                for (i in indices) {
                    val fallback = fallbackMap.remove(this[i])
                    this[i] = create(this[i])
                    fallback?.also { fallbackMap[this[i]] = it }
                }
                finalize(create)
            }
        }
    }

    @androidx.room.Dao
    interface Dao {
        @Query("SELECT * FROM `Profile` WHERE `id` = :id")
        operator fun get(id: Long): Profile?

        @Query("SELECT * FROM `Profile` WHERE `Subscription` != 2 ORDER BY `userOrder`")
        fun listActive(): List<Profile>

        @Query("SELECT * FROM `Profile` WHERE `Subscription` == 2")
        fun listObsolete(): List<Profile>

        @Query("SELECT * FROM `Profile`")
        fun listAll(): List<Profile>

        @Query("SELECT * FROM `Profile` WHERE `url_group` = :group")
        fun listByGroup(group: String): List<Profile>

        @Query("SELECT MAX(`userOrder`) + 1 FROM `Profile`")
        fun nextOrder(): Long?

        @Query("SELECT 1 FROM `Profile` LIMIT 1")
        fun isNotEmpty(): Boolean

        @Insert
        fun create(value: Profile): Long

        @Update
        fun update(value: Profile): Int

        @Query("DELETE FROM `Profile` WHERE `id` = :id")
        fun delete(id: Long): Int

        @Query("DELETE FROM `Profile`")
        fun deleteAll(): Int
    }

    val formattedAddress get() = (if (host.contains(":")) "[%s]:%d" else "%s:%d").format(host, remotePort)
    val formattedName get() = name.ifEmpty { formattedAddress }

    fun copyFeatureSettingsTo(profile: Profile, withMore: Boolean = false) {
        profile.route = route
        profile.remoteDns = remoteDns
        profile.ipv6 = ipv6
        profile.metered = metered
        profile.proxyApps = proxyApps
        profile.bypass = bypass
        profile.individual = individual
        profile.udpdns = udpdns
        if (withMore) {
            profile.name = "$name - copy"
            profile.host = host
            profile.remotePort = remotePort
            profile.password = password
            profile.protocol = protocol
            profile.protocol_param = protocol_param
            profile.obfs = obfs
            profile.obfs_param = obfs_param
            profile.method = method
        }
    }

    fun isSameAs(other: Profile): Boolean = other.host == host && other.remotePort == remotePort &&
            other.password == password && other.method == method &&
            other.protocol == protocol && other.protocol_param == protocol_param &&
            other.obfs == obfs && other.obfs_param == obfs_param &&
            other.name == name && other.url_group == url_group

    override fun toString(): String {
        val flags = Base64.NO_PADDING or Base64.URL_SAFE or Base64.NO_WRAP
        return "ssr://" + Base64.encodeToString("%s:%d:%s:%s:%s:%s/?obfsparam=%s&protoparam=%s&remarks=%s&group=%s"
                .format(Locale.ENGLISH, host, remotePort, protocol, method, obfs,
                        Base64.encodeToString("%s".format(Locale.ENGLISH, password).toByteArray(), flags),
                        Base64.encodeToString("%s".format(Locale.ENGLISH, obfs_param).toByteArray(), flags),
                        Base64.encodeToString("%s".format(Locale.ENGLISH, protocol_param).toByteArray(), flags),
                        Base64.encodeToString("%s".format(Locale.ENGLISH, name).toByteArray(), flags),
                        Base64.encodeToString("%s".format(Locale.ENGLISH, url_group).toByteArray(), flags)).toByteArray(), flags)
    }

    fun toJson(profiles: LongSparseArray<Profile>? = null): JSONObject = JSONObject().apply {
        put("server", host)
        put("server_port", remotePort)
        put("password", password)
        put("method", method)
        put("protocol", protocol)
        put("protocol_param", protocol_param)
        put("obfs", obfs)
        put("obfs_param", obfs_param)
        put("local_address", DataStore.listenAddress)
        put("local_port", DataStore.portProxy)
        put("timeout", 600)

        if (profiles == null) return@apply
        put("remarks", name)
        put("route", route)
        put("remote_dns", remoteDns)
        put("ipv6", ipv6)
        put("metered", metered)
        put("proxy_apps", JSONObject().apply {
            put("enabled", proxyApps)
            if (proxyApps) {
                put("bypass", bypass)
                // android_ prefix is used because package names are Android specific
                put("android_list", JSONArray(individual.split("\n")))
            }
        })
        put("udpdns", udpdns)
        val fallback = profiles.get(udpFallback ?: return@apply)
        if (fallback != null && fallback.plugin.isNullOrEmpty()) fallback.toJson().also { put("udp_fallback", it) }
    }

    fun serialize() {
        DataStore.editingId = id
        DataStore.privateStore.putString(Key.name, name)
        DataStore.privateStore.putString(Key.group, url_group)
        DataStore.privateStore.putString(Key.host, host)
        DataStore.privateStore.putString(Key.remotePort, remotePort.toString())
        DataStore.privateStore.putString(Key.password, password)
        DataStore.privateStore.putString(Key.route, route)
        DataStore.privateStore.putString(Key.remoteDns, remoteDns)
        DataStore.privateStore.putString(Key.protocol, protocol)
        DataStore.privateStore.putString(Key.protocol_param, protocol_param)
        DataStore.privateStore.putString(Key.obfs, obfs)
        DataStore.privateStore.putString(Key.obfs_param, obfs_param)
        DataStore.privateStore.putString(Key.method, method)
        DataStore.proxyApps = proxyApps
        DataStore.bypass = bypass
        DataStore.privateStore.putBoolean(Key.udpdns, udpdns)
        DataStore.privateStore.putBoolean(Key.ipv6, ipv6)
        DataStore.privateStore.putBoolean(Key.metered, metered)
        DataStore.individual = individual
        DataStore.plugin = plugin ?: ""
        DataStore.udpFallback = udpFallback
        DataStore.privateStore.remove(Key.dirty)
    }

    fun deserialize() {
        check(id == 0L || DataStore.editingId == id)
        DataStore.editingId = null
        // It's assumed that default values are never used, so 0/false/null is always used even if that isn't the case
        name = DataStore.privateStore.getString(Key.name) ?: ""
        url_group = DataStore.privateStore.getString(Key.group) ?: ""
        // It's safe to trim the hostname, as we expect no leading or trailing whitespaces here
        host = (DataStore.privateStore.getString(Key.host) ?: "").trim()
        remotePort = parsePort(DataStore.privateStore.getString(Key.remotePort), 8388, 1)
        password = DataStore.privateStore.getString(Key.password) ?: ""
        protocol = DataStore.privateStore.getString(Key.protocol) ?: ""
        protocol_param = DataStore.privateStore.getString(Key.protocol_param) ?: ""
        obfs = DataStore.privateStore.getString(Key.obfs) ?: ""
        obfs_param = DataStore.privateStore.getString(Key.obfs_param) ?: ""
        method = DataStore.privateStore.getString(Key.method) ?: ""
        route = DataStore.privateStore.getString(Key.route) ?: ""
        remoteDns = DataStore.privateStore.getString(Key.remoteDns) ?: ""
        proxyApps = DataStore.proxyApps
        bypass = DataStore.bypass
        udpdns = DataStore.privateStore.getBoolean(Key.udpdns, false)
        ipv6 = DataStore.privateStore.getBoolean(Key.ipv6, false)
        metered = DataStore.privateStore.getBoolean(Key.metered, false)
        individual = DataStore.individual
        plugin = DataStore.plugin
        udpFallback = DataStore.udpFallback
    }
}
