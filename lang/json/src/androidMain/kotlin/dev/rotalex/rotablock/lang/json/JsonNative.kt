package dev.rotalex.rotablock.lang.json

internal object JsonNative {
    external fun jsonLanguage(): Long
}

internal actual fun jsonLanguage(): Long {
    System.loadLibrary("ktreesitter-json")
    return JsonNative.jsonLanguage()
}
