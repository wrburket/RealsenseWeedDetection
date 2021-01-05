# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#################### librealsense dependencies begin ####################
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/include")
add_library(realsense2 SHARED IMPORTED)
set_target_properties(
        realsense2
        PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/build/jni/${ANDROID_ABI}/librealsense2.so
)
####################  librealsense dependencies end  ####################

add_library(native-lib SHARED src/main/cpp/native-lib.cpp)

find_library(log-lib log)

target_link_libraries(
        native-lib
        ${log-lib}
        #################### librealsense dependencies begin ####################
        realsense2
        ####################  librealsense dependencies end  ####################
)