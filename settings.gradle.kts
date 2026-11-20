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

include(":language-api")
include(":block-api")
include(":block-impl")
include(":shared")
include(":androidApp")
include(":desktopApp")
