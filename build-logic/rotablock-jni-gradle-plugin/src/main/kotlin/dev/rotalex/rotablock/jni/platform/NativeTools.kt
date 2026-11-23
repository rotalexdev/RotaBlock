package dev.rotalex.rotablock.jni.platform

import org.gradle.internal.os.OperatingSystem
import java.io.File

internal object NativeTools {

    fun find(name: String): File? =
        OperatingSystem.current()
            .findInPath(name)

    private fun findOrThrow(name: String): File =
        find(name)
            ?: error(
                "$name was not found in PATH. " +
                    "Install $name or make it available in PATH.",
            )

    val cmakePath: String by lazy { findOrThrow("cmake").absolutePath }
}