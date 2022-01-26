cmake_minimum_required(VERSION 3.10.2)

project("ssrDroid")

add_subdirectory(re2)
include_directories(${LIB_RE2_DIR})
add_library( jni-helper SHARED jni-helper.cpp )
target_link_libraries(jni-helper  log re2)

add_subdirectory(libancillary)
add_subdirectory(badvpn)

add_subdirectory(libevent/android)
add_subdirectory(redsocks)


# =============== mbedtls ===============

option(ENABLE_PROGRAMS "Build mbed TLS programs." OFF)
option(ENABLE_TESTING "Build mbed TLS tests." OFF)

add_subdirectory(mbedtls)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/include)
set(MBEDTLS_CRYPTO_LIBRARY mbedcrypto)
set(MBEDTLS_LIBRARY mbedtls)
set(MBEDTLS_X509_LIBRARY mbedx509)


# =============== pcre ===============
#add_subdirectory(pcre/dist)

set(PCRE_LIBRARY pcre)
set(PCRE_ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/pcre)
set(PCRE_INCLUDE_DIR ${PCRE_ROOT_DIR} ${PCRE_ROOT_DIR}/dist)
include_directories(${PCRE_INCLUDE_DIR})
add_definitions(-DHAVE_CONFIG_H)
set(PCRE_SOURCE_FILES
        ${PCRE_ROOT_DIR}/dist/pcre_byte_order.c
        ${PCRE_ROOT_DIR}/dist/pcre_compile.c
        ${PCRE_ROOT_DIR}/dist/pcre_config.c
        ${PCRE_ROOT_DIR}/dist/pcre_dfa_exec.c
        ${PCRE_ROOT_DIR}/dist/pcre_exec.c
        ${PCRE_ROOT_DIR}/dist/pcre_fullinfo.c
        ${PCRE_ROOT_DIR}/dist/pcre_get.c
        ${PCRE_ROOT_DIR}/dist/pcre_globals.c
        ${PCRE_ROOT_DIR}/dist/pcre_jit_compile.c
        ${PCRE_ROOT_DIR}/dist/pcre_maketables.c
        ${PCRE_ROOT_DIR}/dist/pcre_newline.c
        ${PCRE_ROOT_DIR}/dist/pcre_ord2utf8.c
        ${PCRE_ROOT_DIR}/dist/pcre_refcount.c
        ${PCRE_ROOT_DIR}/dist/pcre_string_utils.c
        ${PCRE_ROOT_DIR}/dist/pcre_study.c
        ${PCRE_ROOT_DIR}/dist/pcre_tables.c
        ${PCRE_ROOT_DIR}/dist/pcre_ucd.c
        ${PCRE_ROOT_DIR}/dist/pcre_valid_utf8.c
        ${PCRE_ROOT_DIR}/dist/pcre_version.c
        ${PCRE_ROOT_DIR}/dist/pcre_xclass.c
        ${CMAKE_CURRENT_SOURCE_DIR}/patch/pcre/pcre_chartables.c
        )
add_library(${PCRE_LIBRARY} STATIC ${PCRE_SOURCE_FILES})

add_subdirectory(shadowsocks-libev)
