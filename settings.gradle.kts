pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("rootLibs") {
            from("io.github.alexanderrotela20.catalog:version-catalog:1.2.7")
        }
    }
}

rootProject.name = "rotablock"

include(":core:language")
include(":core:block")
include(":core:engine")
include(":ui:block")
include(":shared")
include(":androidApp")
include(":desktopApp")
