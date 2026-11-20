plugins {
    alias(rootLibs.plugins.convention.android.application.compose)
}

android {
    namespace = "dev.rotalex.rotablock.android"

    defaultConfig {
        applicationId = "dev.rotalex.rotablock"
        versionCode = 1
        versionName = "0.1.0"
    }

    androidResources {
        generateLocaleConfig = false
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
