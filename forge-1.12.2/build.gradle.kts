import dev.architectury.pack200.java.Pack200Adapter

plugins {
    id("gg.essential.loom")
    id("dev.architectury.architectury-pack200")
    id("io.github.goooler.shadow")
}

loom {
    runs {
        getByName("server").runDir("runServer")
    }
    runConfigs.all {
        isIdeConfigGenerated = true
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        // Required because JEI's AT is broken or something
        accessTransformer("jei_at.cfg")
    }
}

configurations.implementation {
    extendsFrom(configurations.getByName("shadow"))
}

dependencies {
    minecraft("com.mojang:minecraft:1.12.2")
    mappings("de.oceanlabs.mcp:mcp_stable:39-1.12")
    forge("net.minecraftforge:forge:1.12.2-14.23.5.2840")

    shadow(project(":core"))

    compileOnly("mezz.jei:jei_1.12.2:4.16.1.302:api")
    runtimeOnly("mezz.jei:jei_1.12.2:4.16.1.302")
}

tasks {
    jar {
        archiveClassifier.set("thin")
        duplicatesStrategy = DuplicatesStrategy.WARN
        dependsOn(shadowJar)
    }

    shadowJar {
        configurations = listOf(project.configurations.getByName("shadow"))
        mergeServiceFiles()
        archiveClassifier.set("fat")
    }
}
