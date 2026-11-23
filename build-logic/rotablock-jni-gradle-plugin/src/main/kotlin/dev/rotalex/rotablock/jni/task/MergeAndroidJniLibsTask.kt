package dev.rotalex.rotablock.jni.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class MergeAndroidJniLibsTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val compiledLibraries: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun merge() {
        val rootOut = outputDirectory.get().asFile
        compiledLibraries.files.forEach { sourceAbiDir ->
            val targetAbiDir = rootOut.resolve(sourceAbiDir.name)
            fileSystemOperations.copy {
                from(sourceAbiDir)
                into(targetAbiDir)
                include("*.so")
            }
        }
    }
}