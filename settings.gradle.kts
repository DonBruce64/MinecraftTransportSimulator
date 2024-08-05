pluginManagement {
    repositories {
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.architectury.dev/") }
        maven { url = uri("https://files.minecraftforge.net/maven/") }
        gradlePluginPortal()
    }
}

rootProject.name = "Immersive Vehicles"
include("mccore")

println("Enabled platforms:")

(ext.get("enabled_platforms") as? String)
    ?.split(',')
    ?.map { platform ->
        val platformTrimmed = platform.trim()
        val module = ":mcinterface${ platformTrimmed.replace(".", "").replace("-", "") }"
        include(module)
        println("- $platformTrimmed ($module)")
        platformTrimmed to module
    }
    .orEmpty()
    .also { gradle.extra["platforms"] = it }