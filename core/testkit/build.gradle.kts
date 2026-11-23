plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.core.testkit"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:language"))
            api(kotlin("test"))
        }
        desktopMain.dependencies {
            api(kotlin("test-junit"))
        }
        androidMain.dependencies {
            api(kotlin("test-junit"))
        }
    }
}
