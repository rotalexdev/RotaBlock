package dev.rotalex.rotablock.jni.model

import dev.rotalex.rotablock.jni.RotablockJniExtension
import dev.rotalex.rotablock.jni.platform.AndroidSdkResolver
import org.gradle.api.Project

internal data class JniTarget(
    val classifier: JniClassifier,
    val cmakeArgs: List<String>,
) {

    companion object {

        fun create(
            project: Project,
            classifier: JniClassifier,
            extension: RotablockJniExtension,
            cmakeConfigExtraArgs: List<String> = emptyList()
        ): JniTarget {
            val platformArgs = if (classifier.isAndroid) {
                androidArgs(project, classifier, extension)
            } else {
                desktopArgs(classifier)
            }
            return JniTarget(
                classifier = classifier,
                cmakeArgs = platformArgs + cmakeConfigExtraArgs + extension.extraArgs.get()
            )
        }

        private fun androidArgs(
            project: Project,
            classifier: JniClassifier,
            extension: RotablockJniExtension,
        ): List<String> {
            val ndk = AndroidSdkResolver.ndk(
                project,
                extension.android.ndkVersion.orNull,
            ) ?: error(
                "Android NDK not found. " +
                    "Set ANDROID_HOME/ANDROID_SDK_ROOT, " +
                    "configure local.properties, or install an NDK.",
            )
            val toolchain = ndk.resolve("build/cmake/android.toolchain.cmake")
                .absolutePath.replace('\\', '/')
            val abi = requireNotNull(classifier.androidAbi) {
                "Classifier $classifier is not configured with an Android ABI"
            }
            return buildList {
                add("-G")
                add("Ninja")
                add("-DCMAKE_BUILD_TYPE=Release")
                add("-DCMAKE_TOOLCHAIN_FILE=$toolchain")
                add("-DANDROID_ABI=$abi")
                add("-DANDROID_PLATFORM=android-${extension.android.minSdk.get()}")
                add("-DCMAKE_SYSTEM_VERSION=${extension.android.minSdk.get()}")
                add("-DANDROID_STL=c++_static")
            }
        }

        private fun desktopArgs(
            classifier: JniClassifier,
        ): List<String> {
            val commonArgs = listOf(
                "-DCMAKE_BUILD_TYPE=Release",
                "-DJAVA_HOME=${System.getProperty("java.home").replace("\\", "/")}"
            )
            val classifierArgs = when (classifier) {
                JniClassifier.LINUX_AARCH64 ->
                    listOf(
                        "-DCMAKE_SYSTEM_NAME=Linux",
                        "-DCMAKE_SYSTEM_PROCESSOR=aarch64",
                        "-DCMAKE_C_COMPILER=aarch64-linux-gnu-gcc",
                        "-DCMAKE_CXX_COMPILER=aarch64-linux-gnu-g++",
                        "-DCMAKE_ASM_COMPILER=aarch64-linux-gnu-gcc",
                    )

                JniClassifier.MACOS_X86_64 ->
                    listOf(
                        "-DCMAKE_OSX_ARCHITECTURES=x86_64",
                        "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0"
                    )

                JniClassifier.MACOS_AARCH64 ->
                    listOf(
                        "-DCMAKE_OSX_ARCHITECTURES=arm64",
                        "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
                    )

                JniClassifier.WINDOWS_X86_64 ->
                    listOf(
                        "-G",
                        "MinGW Makefiles",
                        "-DCMAKE_C_COMPILER=gcc.exe",
                        "-DCMAKE_CXX_COMPILER=g++.exe"
                    )
                else -> emptyList()
            }
        return commonArgs + classifierArgs
        }
    }
}