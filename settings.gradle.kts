pluginManagement {
    repositories {
        maven("https://repo.sk1er.club/repository/maven-releases/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net/")
        mavenCentral()
        gradlePluginPortal()
    }
    
    plugins {
        id("gg.essential.loom") version "1.6.+"
        id("dev.architectury.architectury-pack200") version "0.1.3"
        id("io.github.goooler.shadow") version "8.1.7"
    }
}

include("core", "forge-1.12.2", "forge-1.16.5")

rootProject.name = "Immersive Vehicles"
