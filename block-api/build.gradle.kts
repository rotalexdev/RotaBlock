plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.block.api"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":language-api"))
        }
    }
}
