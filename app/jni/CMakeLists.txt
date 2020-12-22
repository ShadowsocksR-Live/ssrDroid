cmake_minimum_required( VERSION 3.0 )

find_library( log-lib log )

add_subdirectory(libancillary)

#add_subdirectory(pdnsd)

#add_subdirectory(proxychains4)

#add_subdirectory(badvpn)

add_subdirectory(ssrNative)

