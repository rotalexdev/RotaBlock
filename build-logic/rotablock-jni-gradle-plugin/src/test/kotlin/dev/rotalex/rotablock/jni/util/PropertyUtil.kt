package dev.rotalex.rotablock.jni.util

import java.io.File
import java.util.Properties

val kotlinVersion: String = System.getProperty("test.kotlin.version")
    ?: error("test.kotlin.version system property must be set by the test task")
val androidGradlePlugiVersion: String = System.getProperty("test.androidGradlePlugin.version")
    ?: error("test.androidGradlePlugin.version system property must be set by the test task")

val pluginClasspath: String by lazy {

    val resource = object {}.javaClass.classLoader.getResource("plugin-under-test-metadata.properties")
        ?: error("plugin-under-test-metadata.properties not found on test classpath")
    val props = Properties().apply { resource.openStream().use { load(it) } }
    val cp = props.getProperty("implementation-classpath")
        ?: error("implementation-classpath missing from plugin-under-test-metadata.properties")
    cp.split(File.pathSeparator).joinToString(", ") { "files(\"${it.replace("\\", "\\\\")}\")" }
}