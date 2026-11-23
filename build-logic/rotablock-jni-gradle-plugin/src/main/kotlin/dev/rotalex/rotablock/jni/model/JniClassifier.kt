package dev.rotalex.rotablock.jni.model

enum class JniClassifier(
    val id: String,
    val os: Os,
    val architecture: Architecture,
    val libraryExtension: String,
    val androidAbi: String? = null
) {
    LINUX_X86_64(
        id = "linux-x86_64",
        os = Os.LINUX,
        architecture = Architecture.X86_64,
        libraryExtension = ".so",
    ),
    LINUX_AARCH64(
        id = "linux-aarch64",
        os = Os.LINUX,
        architecture = Architecture.AARCH64,
        libraryExtension = ".so",
    ),
    MACOS_X86_64(
        id = "macos-x86_64",
        os = Os.MACOS,
        architecture = Architecture.X86_64,
        libraryExtension = ".dylib",
    ),

    MACOS_AARCH64(
        id = "macos-aarch64",
        os = Os.MACOS,
        architecture = Architecture.AARCH64,
        libraryExtension = ".dylib",
    ),

    WINDOWS_X86_64(
        id = "windows-x86_64",
        os = Os.WINDOWS,
        architecture = Architecture.X86_64,
        libraryExtension = ".dll",
    ),
    ANDROID_ARM64(
        id = "android-arm64-v8a",
        os = Os.ANDROID,
        architecture = Architecture.AARCH64,
        libraryExtension = ".so",
        androidAbi = "arm64-v8a"
    ),
    ANDROID_ARM32(
        id = "android-armeabi-v7a",
        os = Os.ANDROID,
        architecture = Architecture.ARMV7,
        libraryExtension = ".so",
        androidAbi = "armeabi-v7a"
    ),
    ANDROID_X86_64(
        id = "android-x86_64",
        os = Os.ANDROID,
        architecture = Architecture.X86_64,
        libraryExtension = ".so",
        androidAbi = "x86_64"
    ),
    ANDROID_X86(
        id = "android-x86",
        os = Os.ANDROID,
        architecture = Architecture.X86,
        libraryExtension = ".so",
        androidAbi = "x86"
    );

    val isAndroid: Boolean
        get() = os == Os.ANDROID

    val isDesktop: Boolean
        get() = !isAndroid
}

