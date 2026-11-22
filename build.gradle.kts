plugins {
    alias(rootLibs.plugins.android.application) apply false
    alias(rootLibs.plugins.android.kotlin.multiplatform.library) apply false
    alias(rootLibs.plugins.kotlin.multiplatform) apply false
    alias(rootLibs.plugins.kotlin.jvm) apply false
    alias(rootLibs.plugins.kotlin.serialization) apply false
    alias(rootLibs.plugins.compose) apply false
    alias(rootLibs.plugins.compose.compiler) apply false
    alias(rootLibs.plugins.convention.android.application) apply false
    alias(rootLibs.plugins.convention.android.application.compose) apply false
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library) apply false
    alias(rootLibs.plugins.convention.kotlin.multiplatform.library.compose) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
