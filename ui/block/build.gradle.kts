plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library.compose)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.ui.block"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:block"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
