include(
    "firebase-app",
    "firebase-auth",
    "firebase-common",
    "firebase-config",
    "firebase-database",
    "firebase-firestore",
    "firebase-functions",
    "firebase-installations"
)

pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}