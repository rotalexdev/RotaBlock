package dev.rotalex.rotablock.block

import dev.rotalex.rotablock.language.dom.ExtensionPoint

/**
 * The one block mapping point: mappings are registered here explicitly and in order.
 *
 * There is no service loader, no reflection and no global mutable singleton; registration is the only way
 * a mapping enters the projection.
 */
val BLOCK_MAPPING_EP: ExtensionPoint<BlockMapping> = ExtensionPoint("block.mapping")
