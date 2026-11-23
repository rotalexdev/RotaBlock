package dev.rotalex.rotablock.core.testkit

import dev.rotalex.rotablock.core.language.LanguageAdapter

class FakeAdapterConformanceTest : AdapterConformance() {
    override fun adapter(): LanguageAdapter = FakeLanguageAdapter()
}
