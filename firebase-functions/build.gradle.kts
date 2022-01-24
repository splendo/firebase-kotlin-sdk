/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

version = project.property("firebase-functions.version") as String

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    val minSdkVersion: Int by project
    val targetSdkVersion: Int by project

    compileSdkVersion(targetSdkVersion)
    defaultConfig {
        minSdkVersion(minSdkVersion)
        targetSdkVersion(targetSdkVersion)
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }
    packagingOptions {
        pickFirst("META-INF/kotlinx-serialization-core.kotlin_module")
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
    }
    lintOptions {
        isAbortOnError = false
    }
    dependencies {
        val firebaseBoMVersion: String by project
        implementation(platform("com.google.firebase:firebase-bom:$firebaseBoMVersion"))
    }
}

kotlin {

    android {
        publishAllLibraryVariants()
    }

    fun nativeTargetConfig(): KotlinNativeTarget.() -> Unit = {
        val cinteropDir: String by project
        val nativeFrameworkPaths = listOf(
            rootProject.project("firebase-app").projectDir.resolve("$cinteropDir/Carthage/Build/iOS"),
            projectDir.resolve("$cinteropDir/Carthage/Build/iOS")
        )

        binaries {
            getTest("DEBUG").apply {
                linkerOpts(nativeFrameworkPaths.map { "-F$it" })
                linkerOpts("-ObjC")
            }
        }

        compilations.getByName("main") {
            cinterops.create("FirebaseFunctions") {
                compilerOpts(nativeFrameworkPaths.map { "-F$it" })
                extraOpts("-verbose", "-compiler-option", "-DNS_FORMAT_ARGUMENT(A)=")
            }
        }
    }

    if (project.extra["ideaActive"] as Boolean) {
        iosX64("ios", nativeTargetConfig())
    } else {
        ios(configure = nativeTargetConfig())
    }

    js {
        useCommonJs()
        nodejs()
        browser()
    }
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xuse-experimental=kotlin.Experimental",
            "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xuse-experimental=kotlinx.serialization.InternalSerializationApi"
        )
    }

    if (findProperty("firebase-kotlin-sdk.functions.useIR")?.toString()?.equals("true", ignoreCase = true) == true) {
        logger.info("Using IR compilation for firebase-functions module")
        targets.getByName<KotlinAndroidTarget>("android").compilations.all {
            kotlinOptions {
                useIR = true
            }
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "1.4"
                languageVersion = "1.4"
                progressiveMode = true
                useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("com.google.firebase:firebase-functions")
            }
        }

        val iosMain by getting

        val jsMain by getting
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
