rootProject.name = "build-logic"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
	repositories {
		google()
		mavenCentral()
	}

	versionCatalogs {
		create("rootLibs") {
			from("io.github.alexanderrotela20.catalog:version-catalog:1.2.7")
		}
		create("libs") {
			from(files("../gradle/libs.versions.toml"))
		}
	}
}

include(":rotablock-jni-gradle-plugin")
