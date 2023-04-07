## SSRoT Android 客户端

由 Shadowsocks Android 客户端修改而来，支持 [SSRoT 协议](https://github.com/ShadowsocksR-Live/shadowsocksr-native/wiki)。

支持 [overtls](https://github.com/ShadowsocksR-Live/overtls), 一個新代理，相當於去掉 SSR 和 SS, 唯獨保留 OT 的 Rust 實現，速度飛快，穩如老狗。

[常见问题](FAQ.md)

### 编译

#### 工具準備

* [Git](https://git-scm.com/downloads)
* [OpenJDK 11+](https://jdk.java.net/)
* [Android SDK](https://developer.android.com/studio#command-tools)
  - Android SDK Build-Tools 31.0.0
  - Android SDK Platform 33
  - NDK (Side by side) 25.1.8937393
  - Android SDK Tools

* [Rust](https://rust-lang.org/)
  - Install rust on your PC from [rustup](https://rustup.rs/)
  - Some Android targets (arm64, arm, x86_64, x86) for rust.
    ```bash
    rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
    ```

#### 獲取源碼

```shell script
git clone --recursive https://github.com/ShadowsocksR-Live/ssrDroid.git
cd ssrDroid
```

#### 編譯

```shell script
# 在 Windows 下，要設置 JAVA_HOME 環境變量。
set JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"

# 在 macOS 下，设置 JAVA_HOME 环境变量的命令为
export JAVA_HOME="/Applications/Android Studio.app/Contents/jre/Contents/Home"

# 在 Linux 里可以直接使用 系统预安装的 Java 环境，不用再设置自己的 JAVA_HOME 环境变量。

# 下面的 "R" 开关表示 release 编译，去掉 "R" 就编译成 debug 版本。
# 建议编辑 mobile/build.gradle ,修改 applicationId 以规避检测
./gradlew aR
adb install mobile/build/outputs/apk/release/ssrdroid-release.apk
```

### 贡献

欢迎问题修复，功能添加及翻译。其中问题修复和功能添加请优先考虑为上游贡献，翻译请以简体中文为底本。

### 许可

GPLv3

使用的库

<ul>
    <li>redsocks: <a href="https://github.com/shadowsocks/redsocks/blob/shadowsocks-android/README">APL 2.0</a></li>
    <li>mbed TLS: <a href="https://github.com/ARMmbed/mbedtls/blob/development/LICENSE">APL 2.0</a></li>
    <li>libevent: <a href="https://github.com/shadowsocks/libevent/blob/master/LICENSE">BSD</a></li>
    <li>tun2socks: <a href="https://github.com/shadowsocks/badvpn/blob/shadowsocks-android/COPYING">BSD</a></li>
    <li>pcre: <a href="https://android.googlesource.com/platform/external/pcre/+/master/dist2/LICENCE">BSD</a></li>
    <li>libancillary: <a href="https://github.com/shadowsocks/libancillary/blob/shadowsocks-android/COPYING">BSD</a></li>
    <li>ssr-libev: <a href="https://github.com/ssrlive/ssr-libev/blob/master/LICENSE">GPLv3</a></li>
    <li>libsodium: <a href="https://github.com/jedisct1/libsodium/blob/master/LICENSE">ISC</a></li>
</ul>

<ul>
    <li>AndroidX: <a href="https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/LICENSE.txt">APL 2.0</a></li>
    <li>kotlin: <a href="https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt">APL 2.0</a></li>
    <li>material: <a href="https://github.com/material-components/material-components-android/blob/master/LICENSE">APL 2.0</a></li>
    <li>gson: <a href="https://github.com/google/gson/blob/master/LICENSE">APL 2.0</a></li>
    <li>dnsjava: <a href="https://github.com/dnsjava/dnsjava/blob/master/LICENSE">BSD</a></li>
    <li>jsocks: <a href="https://android.googlesource.com/platform/external/pcre/+/master/dist2/LICENCE">LGPL 2.1</a></li>
    <li>preferencex-simplemenu: <a href="https://github.com/takisoft/preferencex-android/blob/master/LICENSE">APL 2.0</a></li>
    <li>android-plugin-api-for-locale: <a href="https://github.com/twofortyfouram/android-plugin-api-for-locale/blob/master/LICENSE.txt">APL 2.0</a></li>
    <li>qrgen: <a href="https://github.com/kenglxn/QRGen">APL 2.0</a></li>
</ul>
