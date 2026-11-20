plugins {
    alias(rootLibs.plugins.kotlin.jvm)
    alias(rootLibs.plugins.compose)
    alias(rootLibs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "dev.rotalex.rotablock.desktop.MainKt"
    }
}
