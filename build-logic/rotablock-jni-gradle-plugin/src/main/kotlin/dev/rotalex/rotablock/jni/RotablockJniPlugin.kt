package dev.rotalex.rotablock.jni

import dev.rotalex.rotablock.jni.model.HostPlatform
import dev.rotalex.rotablock.jni.utils.configureAndroid
import dev.rotalex.rotablock.jni.utils.configureJvm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class RotablockJniPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "rotablockJni",
            RotablockJniExtension::class.java,
        )
        val host = HostPlatform.current()

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            project.afterEvaluate {
                kotlin.targets
                    .filter { it.platformType == KotlinPlatformType.jvm }
                    .forEach { target -> configureJvm(project, kotlin, target.name, extension, host) }
            }
            configureAndroid(project, extension, host)
        }

    }

}