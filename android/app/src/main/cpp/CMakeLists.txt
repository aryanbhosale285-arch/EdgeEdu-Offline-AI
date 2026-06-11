# Built only with: gradle assembleDebug -PenableLlama
# Prerequisite: clone llama.cpp into this directory first:
#   git clone https://github.com/ggml-org/llama.cpp app/src/main/cpp/llama.cpp
cmake_minimum_required(VERSION 3.22.1)
project(edgeedu_llama CXX)

set(CMAKE_CXX_STANDARD 17)

# Keep the on-device build lean: CPU only, no extra tooling.
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(edgeedu_llama SHARED llama_jni.cpp)
target_link_libraries(edgeedu_llama PRIVATE llama common log)
