import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
	`kotlin-dsl`
}

gradlePlugin {
	plugins {
		create("rotablockJni") {
			id = "dev.rotalex.rotablock.jni"
			implementationClass = "dev.rotalex.rotablock.jni.RotablockJniPlugin"
			displayName = "Rotablock JNI"
		}
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
	jvmToolchain(17)
	compilerOptions {
		apiVersion.set(KotlinVersion.KOTLIN_2_4)
		languageVersion.set(KotlinVersion.KOTLIN_2_4)
	}
}

dependencies {
	compileOnly(gradleApi())
	compileOnly(rootLibs.android.gradlePlugin)
	compileOnly(rootLibs.kotlin.gradlePlugin)

	testImplementation(gradleTestKit())
	testImplementation(kotlin("test"))
	testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
	useJUnitPlatform()
	javaLauncher.set(javaToolchains.launcherFor {
		languageVersion.set(JavaLanguageVersion.of(17))
	})
	systemProperty("test.kotlin.version", rootLibs.versions.kotlin.get())
	systemProperty("test.androidGradlePlugin.version", rootLibs.versions.androidGradlePlugin.get())
}

