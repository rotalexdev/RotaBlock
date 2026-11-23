package dev.rotalex.rotablock.jni.utils

import dev.rotalex.rotablock.jni.RotablockJniExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.findByType


private val Project.libs: VersionCatalog?
    get() = extensions.findByType<VersionCatalogsExtension>()?.find("rootLibs")?.orElse(null)

internal fun configureAndroidConventions(
    project: Project,
    extension: RotablockJniExtension
) {
    val resolvedAndroidMinSdk = project.resolveValue(
        key = "androidMinSdk",
        defaultValue = "26"
    ).toInt()
    val resolvedAndroidNdk = project.resolveValue(
        key = "androidNdkVersion",
        defaultValue = "30.0.14904198"
    )

    extension.android.minSdk.convention(resolvedAndroidMinSdk)
    extension.android.ndkVersion.convention(resolvedAndroidNdk)

}

private fun Project.resolveValue(key: String, defaultValue: String): String {
    val fromCatalog = libs?.findVersion(key)?.orElse(null)?.requiredVersion
    if (!fromCatalog.isNullOrBlank()) return fromCatalog
    val fromProperty = findProperty(key)?.toString()
    if (!fromProperty.isNullOrBlank()) return fromProperty
    return defaultValue
}
