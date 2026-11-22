plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library.compose)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.block.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":block-api"))
        }
    }
}
