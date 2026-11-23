plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.parser.treesitter"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:language"))
            api(libs.ktreesitter)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
