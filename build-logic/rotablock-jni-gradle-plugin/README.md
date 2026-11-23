# Rotablock JNI Gradle Plugin

Rotablock JNI is a Gradle plugin for Kotlin Multiplatform projects targeting Android and JVM desktop platforms (Linux, macOS, and Windows). It automates native C/C++ compilation via CMake and packages the resulting shared libraries into Android AARs and JVM JARs without manual task wiring or copying steps.

## Key Features

- **Target Detection:** Compiles JNI libraries based on the host platform for Desktop targets and cross-compiles for all supported Android ABIs using the Android NDK.
- **AGP Variant Integration:** Injects compiled native binaries into Android AAR packaging using `Variant.sources.jniLibs.addGeneratedSourceDirectory` (compatible with `com.android.kotlin.multiplatform.library` and AGP 9).
- **JVM Resource Packaging:** Organizes desktop shared libraries (`.so`, `.dylib`, `.dll`) under target-specific paths within `jvmMain/resources/jni/` to support dynamic loading.
- **Shared C/C++ Source Support:** Supports an optional common directory (`src/commonMain/jni`) shared across Android and JVM builds.
- **Version Catalog Resolution:** Automatically resolves `minSdk` and `ndkVersion` from version catalogs (`rootLibs` or `libs`) and `gradle.properties` with fallback defaults.
- **Configuration Cache Friendly**: Optimized for Gradle Configuration Cache

---

## Requirements

The host build machine must have the following tools available:

- **JDK:** Version 17 or higher.
- **Gradle:** Version 9.x.
- **CMake:** Available via `PATH` or located in the Android SDK directory.
- **Ninja:** Available via `PATH` or located in the Android SDK directory.
- **Android NDK:** Required when targeting Android (configured via `ANDROID_HOME`, `local.properties`, or `ndkVersion` DSL).
- **C/C++ Compiler:**
    - **Linux:** GCC or Clang (`build-essential`).
    - **macOS:** Xcode Command Line Tools (`clang`).
    - **Windows:** MinGW-w64 or Clang toolchain.
---


## Supported Targets and Classifiers

| Platform | Classifier / ABI | Target Architecture | Binary Extension |
| :--- | :--- | :--- | :--- |
| **Android** | `arm64-v8a` | AArch64 | `.so` |
| **Android** | `armeabi-v7a` | ARMv7 | `.so` |
| **Android** | `x86_64` | x86_64 | `.so` |
| **Android** | `x86` | x86 | `.so` |
| **macOS** | `macos-aarch64` | Apple Silicon (M-series) | `.dylib` |
| **macOS** | `macos-x86_64` | Intel 64-bit | `.dylib` |
| **Linux** | `linux-x86_64` | x86_64 | `.so` |
| **Linux** | `linux-aarch64` | AArch64 | `.so` |
| **Windows** | `windows-x86_64` | x86_64 | `.dll` |

---

## Directory Structure

The plugin establishes conventions for source sets across targets:

```text
my-kmp-lib/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   ├── jni/                      # [Optional] Shared source code
    │   │   ├── CMakeLists.txt
    │   │   ├── common.c
    │   │   └── common.h
    │   └── kotlin/
    ├── androidMain/
    │   ├── jni/                      # Android-specific JNI bindings
    │   │   ├── CMakeLists.txt
    │   │   └── android_bridge.c
    │   └── kotlin/
    └── jvmMain/
        ├── jni/                      # JVM Desktop JNI bindings
        │   ├── CMakeLists.txt
        │   └── jvm_bridge.c
        └── kotlin/
```

## Installation and Configuration

### 1. Apply Plugin

Apply the plugin alongside the Kotlin Multiplatform and Android library plugins
in your module's build.gradle.kts:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    id("dev.rotalex.rotablock.jni")
}
```
### 2. Configure Extension

Configure the rotablockJni block. Only libraryName is strictly required;
remaining options provide defaults following standard conventions.
```kotlin
rotablockJni {
    // Required: Name of the generated shared library (without 'lib' prefix)
    libraryName = "my_library"

    // Optional: Extra arguments passed directly to all CMake invocations
    extraArgs = listOf("-DCMAKE_VERBOSE_MAKEFILE=ON")

    // Optional: Location of the shared C/C++ source directory
    // Default: "src/commonMain/jni"
    commonJniDir = "src/commonMain/jni"

    android {
        // Optional: Custom CMakeLists.txt path for Android
        // Default: "src/androidMain/jni/CMakeLists.txt"
        cmakeLists = "src/androidMain/jni/CMakeLists.txt"

        // Optional: Overrides minSdk (resolved from Catalog -> gradle.properties)
        minSdk = 26

        // Optional: Overrides NDK version (resolved from Catalog -> gradle.properties)
        ndkVersion = "30.0.14904198"
    }

    jvm {
        // Optional: Custom CMakeLists.txt path for JVM Desktop
        // Default: "src/jvmMain/jni/CMakeLists.txt"
        cmakeLists = "src/jvmMain/jni/CMakeLists.txt"
    }
}
```

## CMake Integration

The plugin automatically provides predefined variables to your CMakeLists.txt
files depending on the target environment.

### Variables Provided by the Plugin

  - ROTABLOCK_HAS_COMMON: Set to ON if commonJniDir exists on disk; OFF
    otherwise.
  - ROTABLOCK_COMMON_JNI_DIR: Absolute normalized path to commonMain/jni.
  - CMAKE_BUILD_TYPE: Set to Release.
  - ANDROID_ABI, ANDROID_PLATFORM, ANDROID_STL: Injected automatically for
    Android targets via the NDK toolchain file.

### Android CMakeLists.txt Example (src/androidMain/jni/CMakeLists.txt)

```cmake
cmake_minimum_required(VERSION 3.10)
project(android_lib C CXX)

# Include shared core if present
if (ROTABLOCK_HAS_COMMON)
    add_subdirectory(${ROTABLOCK_COMMON_JNI_DIR} common_build)
endif()

find_library(LOG_LIB log)

add_library(
    android_lib
    SHARED
    android_bridge.c
)

target_link_libraries(
    android_lib
    ${LOG_LIB}
)

if (ROTABLOCK_HAS_COMMON)
    target_link_libraries(android_lib common)
endif()
```

### Desktop CMakeLists.txt Example (src/jvmMain/jni/CMakeLists.txt)

```cmake
cmake_minimum_required(VERSION 3.10)
project(jvm_lib C CXX)

if (ROTABLOCK_HAS_COMMON)
    add_subdirectory(${ROTABLOCK_COMMON_JNI_DIR} common_build)
endif()

# Locate JNI headers using JDK path provided by the host environment
find_package(JNI REQUIRED)

add_library(
    jvm_lib
    SHARED
    jvm_bridge.cpp
)

target_include_directories(
    jvm_lib
    PRIVATE
    ${JNI_INCLUDE_DIRS}
)

if (ROTABLOCK_HAS_COMMON)
    target_link_libraries(jvm_lib common)
endif()
```

### Shared CMakeLists.txt Example (src/commonMain/jni/CMakeLists.txt)

```cmake
cmake_minimum_required(VERSION 3.10)
project(common C CXX)

add_library(
    common
    STATIC
    common.c
    common.h
)

target_include_directories(
    common
    PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
)
```

## Generated Artifacts

### Android (AAR Packaging)

When executing ./gradlew assembleRelease, the plugin executes CMake compilation
for each supported Android ABI and packages the resulting .so binaries into
standard Android structure:

```text
build/generated/rotablock-jni/android/jniLibs/
├── arm64-v8a/
│   └── libmy_library.so
├── armeabi-v7a/
│   └── libmy_library.so
├── x86_64/
│   └── libmy_library.so
└── x86/
    └── libmy_library.so
```

These directories are merged into the final AAR artifact via Android Components
Variant APIs.

### JVM Desktop (JAR Packaging)

When executing ./gradlew jvmJar, native binaries matching the host operating
system and architecture are compiled and added directly to the JVM resources:

```text
build/generated/rotablock-jni/jvm/resources/
└── jni/
    ├── macos-aarch64/
    │   └── libmy_library.dylib
    ├── linux-x86_64/
    │   └── libmy_library.so
    └── windows-x86_64/
        └── my_library.dll
```


## Tasks Reference

The plugin registers the following primary lifecycle and compilation tasks:

| Task Name | Description |
| :--- | :--- |
| `compileCmakeAndroid<Abi>` | Compiles the native JNI library for a specific Android ABI (e.g., `compileCmakeAndroidArm64v8a`). |
| `mergeRotablockAndroidJniLibs` | Aggregates all compiled Android `.so` files into the expected `jniLibs` directory structure. |
| `compileCmakeJvm<Target>` | Compiles the native JNI library for the current desktop target (e.g., `compileCmakeJvmMacosAarch64`). |
| `mergeRotablockJvmJniLibs` | Collects desktop shared libraries and packages them into the JVM resources directory. |