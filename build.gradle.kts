import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.InputStream

plugins {
    id("com.android.application") apply false // provided by buildSrc
    alias(libs.plugins.kotlinx.serialization) apply false
    id("org.jetbrains.kotlin.multiplatform") apply false // provided by buildSrc
    id("org.jetbrains.kotlin.native.cocoapods") apply false // provided by buildSrc
    alias(libs.plugins.test.logger.plugin) apply false
    alias(libs.plugins.ben.manes.versions) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.kotlinx.binarycompatibilityvalidator)
    alias(libs.plugins.dokka)
    alias(libs.plugins.publish) apply false
    id("base")
    id("testOptionsConvention")
}

val compileSdkVersion by extra(34)
val targetSdkVersion by extra(34)
val minSdkVersion by extra(23)

private val dokkaCopyrightMessage = "© 2024 GitLive Ltd."
private val dokkaHomepageUrl = "https://github.com/GitLiveApp/firebase-kotlin-sdk"

extensions.configure<DokkaExtension> {
    moduleVersion.set(project.version.toString())
    moduleName.set("Firebase Kotlin SDK")

    pluginsConfiguration.html {
        customAssets.from(file("documentation/gitlive-logo.png"), file("documentation/homepage.svg"))
        customStyleSheets.from(file("documentation/logo-styles.css"))
        footerMessage.set(dokkaCopyrightMessage)
        homepageLink.set(dokkaHomepageUrl)
    }
}

subprojects {

    group = "dev.gitlive"

    val nonDocumentationList = listOf("test-utils", "firebase-common", "firebase-common-internal")
    val skipDocumentation = nonDocumentationList.contains(project.name)
    if (!skipDocumentation) {
        apply(plugin = "org.jetbrains.dokka")

        extensions.configure<DokkaExtension> {
            moduleVersion.set(project.version.toString())

            pluginsConfiguration.html {
                footerMessage.set(dokkaCopyrightMessage)
                separateInheritedMembers.set(false)
                homepageLink.set(dokkaHomepageUrl)
            }

            dokkaSourceSets.configureEach {
                documentedVisibilities.set(setOf(VisibilityModifier.Public))
                includes.from("documentation.md")

                sourceLink {
                    localDirectory.set(projectDir.resolve("src"))
                    remoteUrl("$dokkaHomepageUrl/tree/master/${project.name}/src")
                }
            }
            if (dokkaSourceSets.names.contains("jsMain")) {
                dokkaSourceSets.named("jsMain") {
                    perPackageOption {
                        // External files for JS should not be documented since they will not be available
                        matchingRegex.set(".*.externals.*")
                        suppress.set(true)
                    }
                }
            }
            // In some modules (e.g. firebase-app) jvmMain reuses androidMain's source directory, which makes Dokka's
            // pre-generation check fail on the two source sets sharing a physical source root. The androidMain
            // source set already documents the same code there, so jvmMain is suppressed instead of documented
            // twice - but only when the directories actually overlap, since other modules (e.g. firebase-perf) give
            // jvmMain its own distinct sources that should still be documented.
            // Applied in afterEvaluate since the Kotlin Gradle Plugin adapter populates/overwrites dokkaSourceSets
            // from the real KGP source directories only once project evaluation (incl. the kotlin {} block) completes.
            project.afterEvaluate {
                val kotlinSourceSets = extensions.findByType<KotlinMultiplatformExtension>()?.sourceSets
                val androidMainDirs = kotlinSourceSets?.findByName("androidMain")?.kotlin?.srcDirs.orEmpty()
                val jvmMainDirs = kotlinSourceSets?.findByName("jvmMain")?.kotlin?.srcDirs.orEmpty()
                if (androidMainDirs.isNotEmpty() && jvmMainDirs.isNotEmpty() && androidMainDirs.any { it in jvmMainDirs }) {
                    dokkaSourceSets.named("jvmMain") {
                        // suppress alone does not stop Dokka's pre-generation source-root-overlap check
                        // (https://github.com/Kotlin/dokka/issues/3701), so the shared root is cleared too.
                        sourceRoots.setFrom(emptyList<Any>())
                        suppress.set(true)
                    }
                }
            }
        }
    }

    apply(plugin = "com.adarshr.test-logger")
    apply(plugin = "org.jmailen.kotlinter")

    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !project.gradle.startParameter.taskNames.any { "MavenLocal" in it } }
    }

    val skipPublishing = project.name == "test-utils" // skip publishing for test utils

    tasks {
        withType<Test> {
            testLogging {
                showExceptions = true
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = true
                showCauses = true
                showStackTraces = true
                events = setOf(
                    TestLogEvent.STARTED,
                    TestLogEvent.FAILED,
                    TestLogEvent.PASSED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                    TestLogEvent.STANDARD_ERROR
                )
            }
        }
    }

    afterEvaluate  {
        dependencies {
            "commonMainImplementation"(libs.kotlinx.coroutines.core)
            "androidMainImplementation"(libs.kotlinx.coroutines.play.services)
            // api, not implementation: the Firebase artifacts are declared with `api` and without a
            // version, so the BoM has to reach the api variant as well or a consumer resolving the
            // compile classpath has nothing to supply the version from.
            "androidMainApi"(platform(libs.firebase.bom))
            "commonTestImplementation"(kotlin("test-common"))
            "commonTestImplementation"(kotlin("test-annotations-common"))
            if (this@afterEvaluate.name != "firebase-crashlytics") {
                "jvmMainApi"(libs.gitlive.firebase.java.sdk)
                "jvmMainApi"(libs.kotlinx.coroutines.play.services) {
                    exclude("com.google.android.gms")
                }
                "jsTestImplementation"(kotlin("test-js"))
                "jvmTestImplementation"(kotlin("test-junit"))
                "jvmTestImplementation"(libs.junit)
            }
            "androidInstrumentedTestImplementation"(kotlin("test-junit"))
            "androidUnitTestImplementation"(kotlin("test-junit"))
            "androidInstrumentedTestImplementation"(libs.junit)
            "androidInstrumentedTestImplementation"(libs.androidx.test.core)
            "androidInstrumentedTestImplementation"(libs.androidx.test.junit)
            "androidInstrumentedTestImplementation"(libs.androidx.test.runner)
        }
    }

    if (skipPublishing) return@subprojects

    apply(plugin = "maven-publish")
    apply(plugin = "signing")
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {

    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase(java.util.Locale.ENGLISH).contains(it) }
        val versionMatch = "^[0-9,.v-]+(-r)?$".toRegex().matches(version)

        return (stableKeyword || versionMatch).not()
    }

    rejectVersionIf {
        isNonStable(candidate.version)
    }

    checkForGradleUpdate = true
    outputFormatter = "plain,html"
    outputDir = "build/dependency-reports"
    reportfileName = "dependency-updates"
}
// check for latest dependencies - ./gradlew dependencyUpdates -Drevision=release

tasks.register("devRunAllTests") {
    doLast {
        val gradleTasks = mutableListOf<List<String>>()
        gradleTasks.addAll(EmulatorJobsMatrix().getJvmTestTaskList(rootProject = rootProject))
        gradleTasks.addAll(EmulatorJobsMatrix().getJsTestTaskList(rootProject = rootProject))
        gradleTasks.addAll(EmulatorJobsMatrix().getIosTestTaskList(rootProject = rootProject))
        gradleTasks.add(listOf("ciSdkManagerLicenses"))
        gradleTasks.addAll(EmulatorJobsMatrix().getEmulatorTaskList(rootProject = rootProject))
        gradleTasks.forEach {
            exec {
                executable = File(
                    project.rootDir,
                    if (Os.isFamily(Os.FAMILY_WINDOWS)) "gradlew.bat" else "gradlew",
                )
                    .also { it.setExecutable(true) }
                    .absolutePath
                args = it
                println("exec: ${this.commandLine.joinToString(separator = " ")}")
            }.apply { println("ExecResult: $this") }
        }
    }
}

tasks.register("ciJobsMatrixSetup") {
    doLast {
        EmulatorJobsMatrix().createMatrixJsonFiles(rootProject = rootProject)
    }
}

tasks.register("ciSdkManagerLicenses") {
    doLast {
        val sdkDirPath = getAndroidSdkPath(rootDir = rootDir)
        getSdkmanagerFile(rootDir = rootDir)?.let { sdkmanagerFile ->
            val yesInputStream = object : InputStream() {
                private val yesString = "y\n"
                private var counter = 0
                override fun read(): Int = yesString[counter % 2].also { counter++ }.code
            }
            exec {
                executable = sdkmanagerFile.absolutePath
                args = listOf("--list", "--sdk_root=$sdkDirPath")
                println("exec: ${this.commandLine.joinToString(separator = " ")}")
            }.apply { println("ExecResult: $this") }
            exec {
                executable = sdkmanagerFile.absolutePath
                args = listOf("--licenses", "--sdk_root=$sdkDirPath")
                standardInput = yesInputStream
                println("exec: ${this.commandLine.joinToString(separator = " ")}")
            }.apply { println("ExecResult: $this") }
        }
    }
}
