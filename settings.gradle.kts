pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ⬇️ Tambahkan ini
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ⬇️ Tambahkan ini juga di sini
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DataBuahPKS"
include(":app")
 