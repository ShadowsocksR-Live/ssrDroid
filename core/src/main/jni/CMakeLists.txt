cmake_minimum_required(VERSION 3.10.2)

project("ssrDroid")

add_link_options("LINKER:--build-id=none" "LINKER:-hash-style=gnu")
add_compile_options("-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=ssrDroid")

add_subdirectory(re2)
include_directories(${LIB_RE2_DIR})
add_library( jni-helper SHARED jni-helper.cpp )
target_link_libraries(jni-helper  log re2)

add_subdirectory(fake-dlfcn)
add_subdirectory(ssrNative)
