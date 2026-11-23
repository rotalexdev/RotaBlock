package dev.rotalex.rotablock.jni

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class RotablockJniExtension @Inject constructor() {

    abstract val extraArgs: ListProperty<String>

    @get:Nested
    abstract val common: CommonOptions

    @get:Nested
    abstract val android: AndroidOptions

    @get:Nested
    abstract val jvm: JvmOptions

    fun common(action: Action<CommonOptions>) {
        action.execute(common)
    }

    fun android(action: Action<AndroidOptions>) {
        action.execute(android)
    }

    fun jvm(action: Action<JvmOptions>) {
        action.execute(jvm)
    }

    init {
        extraArgs.convention(emptyList())
    }

    abstract class CommonOptions @Inject constructor() {
        abstract val cmakeLists: Property<String>

        init {
            cmakeLists.convention("src/commonMain/jni/CMakeLists.txt")
        }
    }

    abstract class AndroidOptions @Inject constructor() {
        abstract val minSdk: Property<Int>
        abstract val ndkVersion: Property<String>
        abstract val cmakeLists: Property<String>
        abstract val abis: SetProperty<String>

        init {
            cmakeLists.convention("src/androidMain/jni/CMakeLists.txt")
            abis.convention(setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }

        fun arm64V8a() {
            abis.add("arm64-v8a")
        }

        fun armeabiV7a() {
            abis.add("armeabi-v7a")
        }

        fun x86_64() {
            abis.add("x86_64")
        }

        fun x86() {
            abis.add("x86")
        }

        fun allAbis() {
            abis.addAll("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    abstract class JvmOptions @Inject constructor() {
        abstract val cmakeLists: Property<String>

        init {
            cmakeLists.convention("src/jvmMain/jni/CMakeLists.txt")
        }
    }
}