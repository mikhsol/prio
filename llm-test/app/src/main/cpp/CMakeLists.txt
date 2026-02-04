cmake_minimum_required(VERSION 3.22.1)
project(llama_jni)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# GGML definitions
add_definitions(-DGGML_VERSION="0.0.0")
add_definitions(-DGGML_COMMIT="local")
add_definitions(-DGGML_USE_CPU)

# llama.cpp source directory
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
set(GGML_DIR ${LLAMA_DIR}/ggml)

# Include directories
include_directories(
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/src
    ${LLAMA_DIR}/src/models
    ${LLAMA_DIR}/common
    ${GGML_DIR}/include
    ${GGML_DIR}/src
    ${GGML_DIR}/src/ggml-cpu
    ${GGML_DIR}/src/ggml-cpu/arch/arm
)

# Collect all model source files
file(GLOB MODEL_SOURCES "${LLAMA_DIR}/src/models/*.cpp")

# Collect GGML CPU source files (exclude AMX - x86 only)
file(GLOB GGML_CPU_SOURCES_TOP 
    "${GGML_DIR}/src/ggml-cpu/*.cpp"
    "${GGML_DIR}/src/ggml-cpu/*.c"
)
list(FILTER GGML_CPU_SOURCES_TOP EXCLUDE REGEX ".*amx.*")

# ARM-specific sources for Android
file(GLOB GGML_ARM_SOURCES
    "${GGML_DIR}/src/ggml-cpu/arch/arm/*.cpp"
    "${GGML_DIR}/src/ggml-cpu/arch/arm/*.c"
)

# GGML sources (including gguf.cpp, CPU quants, dl backend)
set(GGML_SOURCES
    ${GGML_DIR}/src/ggml.c
    ${GGML_DIR}/src/ggml-alloc.c
    ${GGML_DIR}/src/ggml-backend.cpp
    ${GGML_DIR}/src/ggml-backend-reg.cpp
    ${GGML_DIR}/src/ggml-backend-dl.cpp
    ${GGML_DIR}/src/ggml-opt.cpp
    ${GGML_DIR}/src/ggml-quants.c
    ${GGML_DIR}/src/ggml-threading.cpp
    ${GGML_DIR}/src/gguf.cpp
    ${GGML_CPU_SOURCES_TOP}
    ${GGML_ARM_SOURCES}
)

# llama.cpp sources
set(LLAMA_SOURCES
    ${LLAMA_DIR}/src/llama.cpp
    ${LLAMA_DIR}/src/llama-adapter.cpp
    ${LLAMA_DIR}/src/llama-arch.cpp
    ${LLAMA_DIR}/src/llama-batch.cpp
    ${LLAMA_DIR}/src/llama-chat.cpp
    ${LLAMA_DIR}/src/llama-context.cpp
    ${LLAMA_DIR}/src/llama-cparams.cpp
    ${LLAMA_DIR}/src/llama-grammar.cpp
    ${LLAMA_DIR}/src/llama-graph.cpp
    ${LLAMA_DIR}/src/llama-hparams.cpp
    ${LLAMA_DIR}/src/llama-impl.cpp
    ${LLAMA_DIR}/src/llama-io.cpp
    ${LLAMA_DIR}/src/llama-kv-cache.cpp
    ${LLAMA_DIR}/src/llama-kv-cache-iswa.cpp
    ${LLAMA_DIR}/src/llama-memory.cpp
    ${LLAMA_DIR}/src/llama-memory-hybrid.cpp
    ${LLAMA_DIR}/src/llama-memory-hybrid-iswa.cpp
    ${LLAMA_DIR}/src/llama-memory-recurrent.cpp
    ${LLAMA_DIR}/src/llama-mmap.cpp
    ${LLAMA_DIR}/src/llama-model.cpp
    ${LLAMA_DIR}/src/llama-model-loader.cpp
    ${LLAMA_DIR}/src/llama-model-saver.cpp
    ${LLAMA_DIR}/src/llama-quant.cpp
    ${LLAMA_DIR}/src/llama-sampling.cpp
    ${LLAMA_DIR}/src/llama-vocab.cpp
    ${LLAMA_DIR}/src/unicode.cpp
    ${LLAMA_DIR}/src/unicode-data.cpp
    ${MODEL_SOURCES}
)

# GGML static library
add_library(ggml STATIC ${GGML_SOURCES})
target_compile_definitions(ggml PRIVATE
    GGML_VERSION="0.0.0"
    GGML_COMMIT="local"
    GGML_USE_CPU
)

# llama.cpp static library
add_library(llama STATIC ${LLAMA_SOURCES})
target_compile_definitions(llama PRIVATE
    GGML_VERSION="0.0.0"
    GGML_COMMIT="local"
    GGML_USE_CPU
    LLAMA_AVAILABLE=1
)
target_link_libraries(llama ggml)

# JNI bridge library
add_library(llama_jni SHARED llama_jni.cpp)
target_compile_definitions(llama_jni PRIVATE LLAMA_AVAILABLE=1)
target_link_libraries(llama_jni
    android
    log
    llama
    ggml
)

find_library(log-lib log)
