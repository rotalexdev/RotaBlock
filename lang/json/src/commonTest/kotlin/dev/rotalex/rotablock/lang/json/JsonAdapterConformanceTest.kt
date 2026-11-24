package dev.rotalex.rotablock.lang.json

import dev.rotalex.rotablock.core.language.LanguageAdapter
import dev.rotalex.rotablock.core.testkit.AdapterConformance

class JsonAdapterConformanceTest : AdapterConformance() {
    override fun adapter(): LanguageAdapter = TreeSitterJson.adapter()

    override fun samples(): List<String> = listOf(
        "",
        "{}",
        "[]",
        "null",
        "{\"a\": [1, -2.5e3, true, false, null]}",
        "{\n  \"a\": \"é€\uD83D\uDE00\"\n}",
        "[1,2,3]",
    )

    override fun malformedSamples(): List<String> = listOf("{", "[1,", "{\"a\": }", "???", "}")
}
