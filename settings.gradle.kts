rootProject.name = "Immersive Vehicles"

pluginManagement {
    pluginManagement {
        repositories {
            maven("https://maven.architectury.dev/") // Architectury
            maven("https://maven.fabricmc.net") // Fabric
            maven("https://maven.minecraftforge.net/") // MinecraftForge
            maven("https://repo.spongepowered.org/maven") // Mixin repo
            maven("https://repo.sk1er.club/repository/maven-releases/") // Essential
            mavenCentral()
            gradlePluginPortal()
        }
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "gg.essential.loom" -> useModule("gg.essential:architectury-loom:1.3.12")
                "dev.architectury.architectury-pack200" -> useModule("dev.architectury:architectury-pack200:0.1.3")
                "com.github.johnrengelman.shadow" -> useModule("com.github.johnrengelman:shadow:8.1.1")
            }
        }
    }
}

include("mccore", "mcinterfaceforge1122", "mcinterfaceforge1165")