package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.block.BlockMapping
import dev.rotalex.rotablock.core.testkit.MappingConformance

class JsonMappingConformanceTest : MappingConformance() {
    override fun mapping(): BlockMapping = JsonBlockMapping()
}
