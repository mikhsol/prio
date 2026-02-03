cmake_minimum_required(VERSION 3.22.1)
project("llama_jni" LANGUAGES CXX)

# C++ Standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ARM64 optimizations for NEON SIMD
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+dotprod+fp16")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG -ffast-math")
    add_definitions(-DGGML_USE_NEON)
endif()

# llama.cpp source files - we include them directly for this test project
# In production, we'd use pre-built libraries
set(LLAMA_DIR ${CMAKE_SOURCE_DIR}/llama.cpp)

# Check if llama.cpp exists, if not create stub for compilation
if(EXISTS ${LLAMA_DIR}/llama.cpp)
    message(STATUS "Using llama.cpp source")
    
    # GGML sources
    set(GGML_SOURCES
        ${LLAMA_DIR}/ggml/src/ggml.c
        ${LLAMA_DIR}/ggml/src/ggml-alloc.c
        ${LLAMA_DIR}/ggml/src/ggml-backend.c
        ${LLAMA_DIR}/ggml/src/ggml-quants.c
    )
    
    # llama sources
    set(LLAMA_SOURCES
        ${LLAMA_DIR}/src/llama.cpp
        ${LLAMA_DIR}/src/llama-vocab.cpp
        ${LLAMA_DIR}/src/llama-grammar.cpp
        ${LLAMA_DIR}/src/llama-sampling.cpp
        ${LLAMA_DIR}/src/unicode.cpp
        ${LLAMA_DIR}/src/unicode-data.cpp
    )
    
    add_library(ggml STATIC ${GGML_SOURCES})
    target_include_directories(ggml PUBLIC ${LLAMA_DIR}/ggml/include)
    
    add_library(llama STATIC ${LLAMA_SOURCES})
    target_include_directories(llama PUBLIC 
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
    )
    target_link_libraries(llama ggml)
    
    set(LLAMA_AVAILABLE TRUE)
else()
    message(STATUS "llama.cpp not found - building stub implementation")
    set(LLAMA_AVAILABLE FALSE)
endif()

# JNI bridge library
add_library(llama_jni SHARED
    llama_jni.cpp
)

target_include_directories(llama_jni PRIVATE
    ${CMAKE_SOURCE_DIR}
)

if(LLAMA_AVAILABLE)
    target_include_directories(llama_jni PRIVATE
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
    )
    target_link_libraries(llama_jni llama)
    target_compile_definitions(llama_jni PRIVATE LLAMA_AVAILABLE=1)
else()
    target_compile_definitions(llama_jni PRIVATE LLAMA_AVAILABLE=0)
endif()

# Link Android libraries
target_link_libraries(llama_jni
    android
    log
)
