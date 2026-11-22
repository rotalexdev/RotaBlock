plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.core.block"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:language"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
