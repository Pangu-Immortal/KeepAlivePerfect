# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/)

add_compile_options(-fno-rtti -O3)

message(@@@${CMAKE_CURRENT_SOURCE_DIR})

#link_directories(binder_libs/${CMAKE_ANDROID_ARCH_ABI})

aux_source_directory(. SRC_LIST) # 搜索当前目录下的所有.cpp文件
aux_source_directory(./utils UTIL_SRC_LIST)

# open follow annotate, if you want to debug the code.
# add_definitions(-DLIB_DEBUG)

add_library( # Sets the name of the library.
        keep_alive

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SRC_LIST} ${UTIL_SRC_LIST})

find_library(log-lib log)

target_link_libraries(
        keep_alive
        ${log-lib})

