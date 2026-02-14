cmake_minimum_required(VERSION 3.10)
project(gametux_core)

add_library(gametux_core SHARED gametux_core.cpp)

target_link_libraries(gametux_core log android)
