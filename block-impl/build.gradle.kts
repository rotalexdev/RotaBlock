plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.block.impl"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":block-api"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
