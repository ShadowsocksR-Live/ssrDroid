cmake_minimum_required(VERSION 3.10.2)

project("ssrDroid")

add_subdirectory(re2)
include_directories(${LIB_RE2_DIR})
add_library( jni-helper SHARED jni-helper.cpp )
target_link_libraries(jni-helper  log re2)

add_subdirectory(libancillary)

add_subdirectory(ssrNative)
