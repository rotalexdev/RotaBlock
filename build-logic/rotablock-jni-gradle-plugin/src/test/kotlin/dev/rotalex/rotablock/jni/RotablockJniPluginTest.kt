package dev.rotalex.rotablock.jni


import dev.rotalex.rotablock.jni.util.setupKmpAndroidProject
import dev.rotalex.rotablock.jni.util.setupKmpCommonProject
import dev.rotalex.rotablock.jni.util.setupKmpJvmProject
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RotablockJniPluginTest {

    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin registers extension successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("dev.rotalex.rotablock.jni")
        val extension = project.extensions.findByName("rotablockJni")

        assertNotNull(extension)
    }

    @Test
    fun `functional test - generates JVM tasks when commonMain CMake exists`() {
        setupKmpCommonProject(testProjectDir, includeJvm = true)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all", "--stacktrace")
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(output.contains("mergeRotablockJvmJniLibs"))
        assertTrue(output.contains("compileCmakeJvm"))
    }

    @Test
    fun `functional test - generates JVM tasks when jvmMain CMake exists`() {
        setupKmpJvmProject(testProjectDir)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all", "--stacktrace")
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(output.contains("mergeRotablockJvmJniLibs"))
        assertTrue(output.contains("compileCmakeJvm"))
    }

    @Test
    fun `functional test - generates Android tasks when commonMain CMake exists`() {
        setupKmpCommonProject(testProjectDir, includeAndroid = true)

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all", "--stacktrace")
            .withPluginClasspath()
            .build()

        val output = result.output
        assertTrue(output.contains("mergeRotablockAndroidJniLibs"))
        assertTrue(output.contains("compileCmakeAndroid"))
    }

    @Test
    fun `functional test - generates Android tasks for arm64-v8a ABI when androidMain CMake exists`() {
        setupKmpAndroidProject(testProjectDir, listOf("arm64V8a()"))

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
            .build()

        val output = result.output

        assertTrue(output.contains("mergeRotablockAndroidJniLibs"))
        assertTrue(output.contains("compileCmakeAndroidArm64v8a"))
        assertFalse(output.contains("compileCmakeAndroidX86_64"))
        assertFalse(output.contains("compileCmakeAndroidX86"))
        assertFalse(output.contains("compileCmakeAndroidArmeabiv7a"))
    }

    @Test
    fun `functional test - generates Android tasks for armeabi-v7a ABI when androidMain CMake exists`() {
        setupKmpAndroidProject(testProjectDir, listOf("armeabiV7a()"))

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
            .build()

        val output = result.output

        assertTrue(output.contains("mergeRotablockAndroidJniLibs"))
        assertTrue(output.contains("compileCmakeAndroidArmeabiv7a"))
        assertFalse(output.contains("compileCmakeAndroidArm64v8a"))
        assertFalse(output.contains("compileCmakeAndroidX86_64"))
        assertFalse(output.contains("compileCmakeAndroidX86"))
    }

    @Test
    fun `functional test - generates Android tasks for x86_64 ABI when androidMain CMake exists`() {
        setupKmpAndroidProject(testProjectDir, listOf("x86_64()"))

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
            .build()

        val output = result.output

        assertTrue(output.contains("mergeRotablockAndroidJniLibs"))
        assertTrue(output.contains("compileCmakeAndroidX86_64"))
        assertFalse(output.contains("compileCmakeAndroidArm64v8a"))
        assertFalse(output.contains("compileCmakeAndroidArmeabiv7a"))
    }

    @Test
    fun `functional test - generates Android tasks for x86 ABI when androidMain CMake exists`() {
        setupKmpAndroidProject(testProjectDir, listOf("x86()"))

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--stacktrace")
            .build()

        val output = result.output

        assertTrue(output.contains("mergeRotablockAndroidJniLibs"))
        assertTrue(output.contains("compileCmakeAndroidX86"))
        assertFalse(output.contains("compileCmakeAndroidArm64v8a"))
        assertFalse(output.contains("compileCmakeAndroidArmeabiv7a"))
        assertFalse(output.contains("compileCmakeAndroidX86_64"))
    }

}
