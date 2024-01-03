pluginManagement {
    pluginManagement {
        repositories {
            maven("https://maven.minecraftforge.net/")
            maven("https://maven.gofancy.wtf/releases") // FancyGradle repo
            maven("https://maven.parchmentmc.org") // Parchment mappings plugin repo
            maven("https://repo.spongepowered.org/maven") // Mixin repo
            gradlePluginPortal()
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }
}

rootProject.name = "Immersive Vehicles"
include("mccore", "mcinterfaceforge1122", "mcinterfaceforge1165")