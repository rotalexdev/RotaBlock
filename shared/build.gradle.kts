plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library.compose)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.shared"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:language"))
            api(project(":core:block"))
            implementation(project(":core:engine"))
            implementation(project(":ui:block"))
            implementation(project(":lang:json"))
            implementation(rootLibs.jetbrains.compose.ui.tooling.preview)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
