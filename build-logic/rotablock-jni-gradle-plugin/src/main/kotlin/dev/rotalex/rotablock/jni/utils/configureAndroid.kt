package dev.rotalex.rotablock.jni.utils

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import dev.rotalex.rotablock.jni.RotablockJniExtension
import dev.rotalex.rotablock.jni.model.HostPlatform
import dev.rotalex.rotablock.jni.model.JniClassifier
import dev.rotalex.rotablock.jni.model.JniTarget
import dev.rotalex.rotablock.jni.task.CmakeBuildTask
import dev.rotalex.rotablock.jni.task.MergeAndroidJniLibsTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider


internal fun configureAndroid(
    project: Project,
    extension: RotablockJniExtension,
    host: HostPlatform,
) {
    project.plugins.withId("com.android.kotlin.multiplatform.library") {
        configureAndroidConventions(project, extension)

        val platformFile = project.file(extension.android.cmakeLists.get())
        val commonFile = project.file(extension.common.cmakeLists.get())

        val cmakeSourceDir = when {
            platformFile.isFile -> platformFile.parentFile
            commonFile.isFile -> commonFile.parentFile
            else -> return@withId
        }

        val mergeJniLibsTask = project.tasks.register(
            "mergeRotablockAndroidJniLibs",
            MergeAndroidJniLibsTask::class.java,
        ) {
            group = "rotablock-jni"
            description = "Merge Android JNI libraries into the jniLibs directory structure"

            outputDirectory.set(
                project.layout.buildDirectory.dir("generated/rotablock-jni/android/jniLibs")
            )
        }

        val androidComponents = project.extensions.getByType(
            KotlinMultiplatformAndroidComponentsExtension::class.java
        )
        androidComponents.onVariants { variant ->
            variant.sources.jniLibs?.addGeneratedSourceDirectory(
                mergeJniLibsTask,
                MergeAndroidJniLibsTask::outputDirectory
            )
        }

        project.afterEvaluate {
            val userAbis = extension.android.abis.get()
            val classifiers = listOf(
                JniClassifier.ANDROID_ARM64,
                JniClassifier.ANDROID_ARM32,
                JniClassifier.ANDROID_X86_64,
                JniClassifier.ANDROID_X86
            )
                .filter(host::canBuild)
                .filter { it.androidAbi in userAbis }

            classifiers.forEach { classifier ->
                val abi = classifier.androidAbi ?: return@forEach
                val compileTask: TaskProvider<CmakeBuildTask> = project.tasks.register(
                    "compileCmakeAndroid${
                        abi.replace("-", "").replaceFirstChar { it.uppercase() }
                    }",
                    CmakeBuildTask::class.java,
                ) {
                    group = "rotablock-jni"
                    description = "Build JNI libraries for Android using CMake ($abi)"

                    sourceDir.set(cmakeSourceDir)
                    if (commonFile.parentFile.isDirectory) commonSourceDir.set(commonFile.parentFile)
                    cmakeArgs.set(project.provider {
                        JniTarget.create(
                            project,
                            classifier,
                            extension
                        ).cmakeArgs
                    })
                    libraryExtension.set(classifier.libraryExtension)
                    workingDir.set(
                        project.layout.buildDirectory.dir("intermediates/rotablock-jni/cmake/android/$abi")
                    )
                    outputDirectory.set(
                        project.layout.buildDirectory.dir("intermediates/rotablock-jni/android/$abi")
                    )
                }

                mergeJniLibsTask.configure {
                    compiledLibraries.from(compileTask.flatMap { it.outputDirectory })
                }
            }
        }
    }
}
