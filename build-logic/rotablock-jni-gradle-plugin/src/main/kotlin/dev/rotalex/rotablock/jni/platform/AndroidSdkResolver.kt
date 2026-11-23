package dev.rotalex.rotablock.jni.platform

import dev.rotalex.rotablock.jni.model.Architecture
import dev.rotalex.rotablock.jni.model.HostPlatform
import dev.rotalex.rotablock.jni.model.Os
import org.gradle.api.Project
import java.io.File
import java.util.Properties

internal object AndroidSdkResolver {

    fun sdk(project: Project): File? = sequenceOf(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        localProperties(project),
        defaultSdk().path,
    )
        .filterNotNull()
        .map(::File)
        .firstOrNull(File::isDirectory)

    fun ndk(
        project: Project,
        version: String? = null,
    ): File? {
        val ndkRoot = File(sdk(project) ?: return null, "ndk")
        if (!ndkRoot.isDirectory) return null
        if (version != null) {
            return File(ndkRoot, version).takeIf(File::isDirectory)
        }
        return ndkRoot
            .listFiles()
            ?.filter(File::isDirectory)
            ?.maxWithOrNull(
                compareBy(
                    { it.ndkPart(0) },
                    { it.ndkPart(1) },
                    { it.ndkPart(2) }
                )
            )
    }

    fun hostTag(): String =
        when (val host = HostPlatform.current()) {
            HostPlatform(
                Os.LINUX,
                Architecture.X86_64,
            ) -> "linux-x86_64"

            HostPlatform(
                Os.LINUX,
                Architecture.AARCH64,
            ) -> "linux-aarch64"

            HostPlatform(
                Os.WINDOWS,
                Architecture.X86_64,
            ) -> "windows-x86_64"

            HostPlatform(
                Os.MACOS,
                Architecture.X86_64,
            ) -> "darwin-x86_64"

            HostPlatform(
                Os.MACOS,
                Architecture.AARCH64,
            ) -> "darwin-arm64"
            else -> error("Unsupported Android NDK host: $host")
        }

    private fun localProperties(project: Project): String? =
        project.rootProject
            .file("local.properties")
            .takeIf(File::isFile)
            ?.let { file ->
                Properties()
                    .apply {
                        file.inputStream().use(::load)
                    }
                    .getProperty("sdk.dir")
            }

    private fun defaultSdk(): File {
        val home = File(System.getProperty("user.home"))
        return when (HostPlatform.current().os) {
            Os.WINDOWS ->
                File(
                    System.getenv("LOCALAPPDATA")
                        ?: File(home, "AppData/Local").path,
                    "Android/Sdk",
                )
            Os.MACOS -> File(home, "Library/Android/sdk")
            Os.LINUX -> File(home, "Android/Sdk")
            else -> {
                error("Unsupported host operating system: ${HostPlatform.current().os}")
            }
        }
    }

    private fun File.ndkPart(index: Int): Long =
        name.split('.').getOrNull(index)?.toLongOrNull() ?: 0L
}