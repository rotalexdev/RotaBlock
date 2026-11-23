plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.core.engine"
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
