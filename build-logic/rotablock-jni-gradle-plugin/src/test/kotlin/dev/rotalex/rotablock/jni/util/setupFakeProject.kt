package dev.rotalex.rotablock.jni.util

import java.io.File


fun setupKmpCommonProject(
    projectDir: File,
    includeAndroid: Boolean = false,
    includeJvm: Boolean = false
) {
    File(projectDir, "settings.gradle.kts").writeText(
        """
            rootProject.name = "test-common"
            pluginManagement {
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            """.trimIndent()
    )

    File(projectDir, "build.gradle.kts").writeText(
        """
            buildscript {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath("com.android.tools.build:gradle:$androidGradlePlugiVersion")
                    classpath(files($pluginClasspath))
                }
            }

            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            ${if (includeAndroid) """apply(plugin = "com.android.kotlin.multiplatform.library")""" else ""}
            apply(plugin = "dev.rotalex.rotablock.jni")

            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                ${if (includeJvm) "jvm()" else ""}
                ${
                    if (includeAndroid) {
                """
                   (this as org.gradle.api.plugins.ExtensionAware).extensions
                    .configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension>("android") {
                        namespace = "dev.rotalex.rotablock.jni.test"
                        compileSdk = 37
                        minSdk = 26
                    }
                    """.trimIndent()
                    } else ""
                }
            }
            """.trimIndent()
    )
    val commonCmake = File(projectDir, "src/commonMain/jni/CMakeLists.txt")
    commonCmake.parentFile.mkdirs()
    commonCmake.writeText("cmake_minimum_required(VERSION 3.22)\nproject(test_common)")
}


fun setupKmpAndroidProject(
    projectDir: File,
    abiFilters: List<String> = listOf("arm64V8a()", "armeabiV7a()", "x86_64()", "x86()")
) {
    File(projectDir, "settings.gradle.kts").writeText(
        """
            rootProject.name = "test-android"
            pluginManagement {
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            dependencyResolutionManagement {
            	repositories {
            		google()
            		mavenCentral()
            	}
            }
            """.trimIndent()
    )

    File(projectDir, "build.gradle.kts").writeText(
        """
            buildscript {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath("com.android.tools.build:gradle:$androidGradlePlugiVersion")
                    classpath(files($pluginClasspath))
                }
            }

            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "com.android.kotlin.multiplatform.library")
            apply(plugin = "dev.rotalex.rotablock.jni")

            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                jvm()
                (this as org.gradle.api.plugins.ExtensionAware).extensions
                    .configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension>("android") {
                        namespace = "dev.rotalex.rotablock.jni.test"
                        compileSdk = 37
                        minSdk = 26
                    }
            }

            extensions.configure<dev.rotalex.rotablock.jni.RotablockJniExtension> {
                android {
                    ${abiFilters.joinToString("\n")}
                }
            }
            """.trimIndent()
    )
    val androidCmake = File(projectDir, "src/androidMain/jni/CMakeLists.txt")
    androidCmake.parentFile.mkdirs()
    androidCmake.writeText("cmake_minimum_required(VERSION 3.22)\nproject(test_android)")
    File(projectDir, "local.properties").writeText("sdk.dir=/fake/path")
}

fun setupKmpJvmProject(projectDir: File) {
    val jvmCmake = File(projectDir, "src/jvmMain/jni/CMakeLists.txt")
    jvmCmake.parentFile.mkdirs()
    jvmCmake.writeText("cmake_minimum_required(VERSION 3.22)\nproject(test_jvm)")

    File(projectDir, "settings.gradle.kts").writeText(
        """
            rootProject.name = "test-jvm"
            pluginManagement {
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            """.trimIndent()
    )
    File(projectDir, "build.gradle.kts").writeText(
        """
            buildscript {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
                dependencies {
                    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                    classpath("com.android.tools.build:gradle:$androidGradlePlugiVersion")
                    classpath(files($pluginClasspath))
                }
            }

            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "dev.rotalex.rotablock.jni")

            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                jvm()
            }

            extensions.configure<dev.rotalex.rotablock.jni.RotablockJniExtension>("rotablockJni") {
              jvm {
                cmakeLists = "${jvmCmake.invariantSeparatorsPath}"
              }
            }
            """.trimIndent()
    )
}