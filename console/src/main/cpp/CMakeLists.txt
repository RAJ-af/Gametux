cmake_minimum_required(VERSION 3.22.1)
project("gametux_core")

add_library(gametux_core SHARED gametux_core.cpp)

find_library(log-lib log)
target_link_libraries(gametux_core ${log-lib} android)
