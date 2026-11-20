plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library.compose)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.shared"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":language-api"))
            api(project(":block-api"))
            implementation(project(":block-impl"))
        }
    }
}
