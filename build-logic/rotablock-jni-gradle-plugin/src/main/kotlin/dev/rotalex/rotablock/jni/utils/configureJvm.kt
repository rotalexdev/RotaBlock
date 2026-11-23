package dev.rotalex.rotablock.jni.utils

import dev.rotalex.rotablock.jni.RotablockJniExtension
import dev.rotalex.rotablock.jni.model.HostPlatform
import dev.rotalex.rotablock.jni.model.JniClassifier
import dev.rotalex.rotablock.jni.model.JniTarget
import dev.rotalex.rotablock.jni.task.CmakeBuildTask
import dev.rotalex.rotablock.jni.task.MergeJvmJniLibsTask
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun configureJvm(
    project: Project,
    kotlin: KotlinMultiplatformExtension,
    extension: RotablockJniExtension,
    host: HostPlatform,
) {
    val platformFile = project.file(extension.jvm.cmakeLists.get())
    val commonFile = project.file(extension.common.cmakeLists.get())

    val cmakeSourceDir = when {
        platformFile.isFile -> platformFile.parentFile
        commonFile.isFile -> commonFile.parentFile
        else -> return
    }

    val desktopClassifiers = listOf(
        JniClassifier.LINUX_X86_64,
        JniClassifier.LINUX_AARCH64,
        JniClassifier.MACOS_X86_64,
        JniClassifier.MACOS_AARCH64,
        JniClassifier.WINDOWS_X86_64,
    ).filter(host::canBuild)

    if (desktopClassifiers.isEmpty()) return

    val mergeJvmLibsTask = project.tasks.register(
        "mergeRotablockJvmJniLibs",
        MergeJvmJniLibsTask::class.java,
    ) {
        group = "rotablock-jni"
        description = "Merge Desktop JNI libraries into the JVM resources directory."
        outputDirectory.set(
            project.layout.buildDirectory.dir("generated/rotablock-jni/jvm/resources")
        )
    }

    desktopClassifiers.forEach { classifier ->
        val taskNameSuffix = classifier.name.split('_').joinToString("") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }

        val compileTask = project.tasks.register(
            "compileCmakeJvm$taskNameSuffix",
            CmakeBuildTask::class.java,
        ) {
            group = "rotablock-jni"
            description = "Build JNI libraries for Desktop using CMake (${classifier.id})\n"


            sourceDir.set(cmakeSourceDir)
            if (commonFile.isFile) commonSourceDir.set(commonFile.parentFile)
            cmakeArgs.set(project.provider { JniTarget.create( project, classifier, extension).cmakeArgs })
            libraryExtension.set(classifier.libraryExtension)
            workingDir.set(
                project.layout.buildDirectory.dir("intermediates/rotablock-jni/cmake/jvm/${classifier.id}")
            )
            outputDirectory.set(
                project.layout.buildDirectory.dir("intermediates/rotablock-jni/jvm/${classifier.id}")
            )
        }

        mergeJvmLibsTask.configure {
            compiledLibraries.from(compileTask.flatMap { it.outputDirectory })
        }
    }

    kotlin.sourceSets.matching { it.name == "jvmMain" }.configureEach {
        resources.srcDir(mergeJvmLibsTask.flatMap { it.outputDirectory })
    }
}