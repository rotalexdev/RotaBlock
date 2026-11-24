package dev.rotalex.rotablock.lang.json

import java.io.File
import java.nio.file.Files

internal object JsonNative {
    external fun jsonLanguage(): Long
}

private val loaded: Unit by lazy {
    val name = "ktreesitter-json"
    val extension = when {
        System.getProperty("os.name").lowercase().contains("windows") -> "dll"
        System.getProperty("os.name").lowercase().contains("mac") -> "dylib"
        else -> "so"
    }
    val classifier = when {
        System.getProperty("os.name").lowercase().contains("windows") -> "windows-x86_64"
        System.getProperty("os.name").lowercase().contains("mac") ->
            if (System.getProperty("os.arch").contains("aarch64")) "macos-aarch64" else "macos-x86_64"
        System.getProperty("os.arch").contains("aarch64") -> "linux-aarch64"
        else -> "linux-x86_64"
    }
    val loader = JsonNative::class.java.classLoader
    val stream = listOf("lib$name.$extension", "$name.$extension")
        .map { fileName -> "jni/$classifier/$fileName" }
        .firstNotNullOfOrNull(loader::getResourceAsStream)
        ?: error("Native library for $classifier is missing")
    val file = Files.createTempFile(name, ".$extension").toFile()
    file.deleteOnExit()
    stream.use { input -> file.outputStream().use(input::copyTo) }
    System.load(file.absolutePath)
}

internal actual fun jsonLanguage(): Long {
    loaded
    return JsonNative.jsonLanguage()
}
