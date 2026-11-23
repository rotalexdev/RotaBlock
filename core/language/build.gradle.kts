plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.core.language"
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
