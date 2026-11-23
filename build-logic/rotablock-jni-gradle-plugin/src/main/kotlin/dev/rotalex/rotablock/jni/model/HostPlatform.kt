package dev.rotalex.rotablock.jni.model

import org.gradle.internal.os.OperatingSystem

internal data class HostPlatform(
    val os: Os,
    val architecture: Architecture,
) {
    companion object {
        fun current(): HostPlatform {
            val operatingSystem = OperatingSystem.current()
            val archProperty = System.getProperty("os.arch").lowercase()

            val os = when {
                operatingSystem.isWindows -> Os.WINDOWS
                operatingSystem.isMacOsX -> Os.MACOS
                operatingSystem.isLinux -> Os.LINUX
                else -> error("Unsupported host operating system: $operatingSystem")
            }

            val arch = when (archProperty) {
                "x86_64", "amd64" -> Architecture.X86_64
                "aarch64", "arm64" -> Architecture.AARCH64
                "x86", "i386", "i686" -> Architecture.X86
                else -> error("Unsupported host architecture: $archProperty")
            }

            return HostPlatform(os, arch)
        }
    }

    fun canBuild(target: JniClassifier): Boolean {
        if (target.isAndroid) return true
        if (this.os != target.os) return false
        return when (this.os) {
            Os.MACOS -> target.architecture in listOf(Architecture.X86_64, Architecture.AARCH64)
            Os.LINUX, Os.WINDOWS -> this.architecture == target.architecture
            else -> false
        }
    }
}