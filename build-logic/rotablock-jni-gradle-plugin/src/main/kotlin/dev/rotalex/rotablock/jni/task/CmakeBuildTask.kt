package dev.rotalex.rotablock.jni.task

import dev.rotalex.rotablock.jni.platform.NativeTools
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "CMake manages its own incremental build cache internally")
abstract class CmakeBuildTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val commonSourceDir: DirectoryProperty

    @get:Input
    abstract val cmakeArgs: ListProperty<String>

    @get:Input
    abstract val libraryExtension: Property<String>

    @get:Internal
    abstract val workingDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun build() {
        val cmake = NativeTools.cmakePath

        val workDir = workingDir.get().asFile
        val srcDir = sourceDir.get().asFile
        val outDir = outputDirectory.get().asFile
        val libExt = libraryExtension.get()
        workDir.mkdirs()
        outDir.mkdirs()

        val dynamicArgs = mutableListOf<String>()

        if (commonSourceDir.isPresent) {
            val commonDir = commonSourceDir.get().asFile
            if (commonDir.absolutePath != srcDir.absolutePath) {
                val commonDirNormalized = commonDir.absolutePath.replace('\\', '/')
                val injectionFile = workDir.resolve("rotablock_common.cmake")
                injectionFile.writeText(
                    """
                    # DO NOT EDIT. This file is generated.
                    cmake_language(DEFER CALL rotablock_inject_common_code)

                    function(rotablock_inject_common_code)
                        get_directory_property(ALL_TARGETS BUILDSYSTEM_TARGETS)
                        
                        set(TARGET_TO_INJECT "")
                        foreach(T IN LISTS ALL_TARGETS)
                            get_target_property(TARGET_TYPE ${'$'}{T} TYPE)
                            if(TARGET_TYPE STREQUAL "SHARED_LIBRARY")
                                set(TARGET_TO_INJECT ${'$'}{T})
                                break()
                            endif()
                        endforeach()

                        if (TARGET_TO_INJECT)
                            target_include_directories(${'$'}{TARGET_TO_INJECT} PRIVATE "$commonDirNormalized")

                            file(GLOB_RECURSE _ROTABLOCK_COMMON_SOURCES 
                                "$commonDirNormalized/*.c" 
                                "$commonDirNormalized/*.cpp" 
                                "$commonDirNormalized/*.cc" 
                                "$commonDirNormalized/*.cxx"
                            )
                            if (_ROTABLOCK_COMMON_SOURCES)
                                target_sources(${'$'}{TARGET_TO_INJECT} PRIVATE ${'$'}{_ROTABLOCK_COMMON_SOURCES})
                            endif()
                        else()
                        endif()
                    endfunction()
                    """.trimIndent()
                )

                dynamicArgs.add("-DCMAKE_PROJECT_INCLUDE=${injectionFile.absolutePath.replace('\\', '/')}")
            }
        }

        execOperations.exec {
            workingDir = workDir
            commandLine(cmake)
            args("-S", srcDir.absolutePath)
            args("-B", workDir.absolutePath)
            args(dynamicArgs)
            args(cmakeArgs.get())
        }
        execOperations.exec {
            workingDir = workDir
            commandLine(cmake)
            args("--build", workDir.absolutePath)
            args("--config", "Release")
        }

        val builtLibraries = workDir.walkTopDown()
            .filter { !it.absolutePath.contains("CMakeFiles") }
            .filter { it.isFile && it.name.endsWith(libExt) }
            .toList()

        require(builtLibraries.isNotEmpty()) {
            "Compiled library with extension '$libExt' not found in $workDir"
        }

        fileSystemOperations.copy {
            from(builtLibraries)
            into(outDir)
        }
    }
}