package dev.rotalex.rotablock.jni.model

enum class Os(
    val nativeName: String
) {
    LINUX("Linux"),
    WINDOWS("Windows"),
    MACOS("Darwin"),
    ANDROID("Android")
}