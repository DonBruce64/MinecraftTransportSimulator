plugins {
    id("gg.essential.loom")
    id("dev.architectury.architectury-pack200")
    id("com.github.johnrengelman.shadow")
    id("io.github.juuxel.loom-vineflower")
}

val jeiVersion: String by project

loom {
    runs.getByName("server").runDir("runServer")
    runConfigs.all {
        isIdeConfigGenerated = true
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        accessTransformer("jei_at.cfg") // JEI for 1.12.2 is just broken or something, specify copy of its AT
    }
}

val shade: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    minecraft("com.mojang:minecraft:1.12.2")
    mappings("de.oceanlabs.mcp:mcp_stable:39-1.12")
    // 2840 is the most recent compatible version with E Loom
    forge("net.minecraftforge:forge:1.12.2-14.23.5.2840")

    api(project(":mccore"))

    shade(project(":mccore"))
    shade("com.googlecode.soundlibs:jlayer:1.0.1.4")
    shade("org.jcraft:jorbis:0.0.17")

    compileOnly("mezz.jei:jei_$jeiVersion:api")
    runtimeOnly("mezz.jei:jei_$jeiVersion")
}

tasks {
    processResources {
        project.projectDir.walkTopDown().forEach { file ->
            if (file.name in listOf("mcmod.info", "InterfaceLoader.java")) {
                var content = file.readText()
                content = content.replace(Regex("\"version\": \"[^\"]*\""), "\"version\": \"${rootProject.properties["globalVersion"]}\"")
                content = content.replace(Regex("MODVER = \"[^\"]*\";"), "MODVER = \"${rootProject.properties["globalVersion"]}\";")
                file.writeText(content)
            }
        }
    }
    shadowJar {
        configurations = listOf(shade)
    }
    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
    }
    assemble.get().dependsOn(remapJar)
}
