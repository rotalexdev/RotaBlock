package dev.rotalex.rotablock.core.language

/**
 * The only registration mechanism of the framework: an ordered list of extensions of one type.
 *
 * Registration is explicit and ordered, and a point owns its own registrations, so no service loader, no
 * reflection and no mutable global state stands behind it.
 *
 * @param T the type of extension this point holds.
 */
class ExtensionPoint<T>(val id: String) {
    init {
        require(id.isNotBlank()) { "An extension point id must not be blank." }
    }

    private val registered = mutableListOf<T>()

    /** Appends [extension] after every extension registered so far. */
    fun register(extension: T) {
        registered.add(extension)
    }

    /** The registered extensions in registration order, as a snapshot later registrations do not change. */
    fun extensions(): List<T> = registered.toList()
}
