plugins {
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library)
    id("dev.rotalex.rotablock.jni")
}

kotlin {
    android {
        namespace = "dev.rotalex.rotablock.lang.json"
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:block"))
            api(project(":parser:tree-sitter"))
            api(libs.ktreesitter)
        }
        commonTest.dependencies {
            implementation(project(":core:engine"))
            implementation(project(":core:testkit"))
            implementation(kotlin("test"))
        }
    }
}

rotablockJni {
    jvm {
        cmakeLists = "src/desktopMain/jni/CMakeLists.txt"
    }

    android {
        abis.set(setOf("arm64-v8a", "armeabi-v7a", "x86_64"))
    }
}
