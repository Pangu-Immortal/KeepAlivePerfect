# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include/)

add_compile_options(-fno-rtti -O3)

message(@@@${CMAKE_CURRENT_SOURCE_DIR})

link_directories(binder_libs/${CMAKE_ANDROID_ARCH_ABI})

aux_source_directory(. SRC_LIST) # 搜索当前目录下的所有.cpp文件

add_library( # Sets the name of the library.
        keep_alive

        # Sets the library as a shared library.
        SHARED

        # 提供源文件的相对路径。
        ${SRC_LIST} main.cpp art.cpp)

find_library(log-lib log)

target_link_libraries(
        keep_alive
        ${log-lib} binder cutils utils c)

