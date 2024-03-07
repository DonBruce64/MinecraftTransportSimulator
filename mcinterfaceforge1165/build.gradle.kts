plugins {
    id("gg.essential.loom")
    id("io.github.juuxel.loom-vineflower")
}

// These values come from project properties (gradle.properties)
val tweakClass: String by project
val mixinConfig: String by project
val mixinRefmap: String by project
val jeiVersion: String by project

loom {
    runs.getByName("server").runDir("runServer")
    runConfigs.all {
        isIdeConfigGenerated = true
    }
    forge {
        mixinConfig(mixinConfig)
    }
    @Suppress("UnstableApiUsage")
    mixin {
        defaultRefmapName.set(mixinRefmap)
    }
}

val shade: Configuration by configurations.creating {
    isTransitive = false
}

repositories {
    maven("https://maven.parchmentmc.org") // Parchment mappings
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.16.5:2022.03.06@zip")
    })
    forge("net.minecraftforge:forge:1.16.5-36.2.41")

    api(project(":mccore"))

    shade(project(":mccore"))
    shade("com.googlecode.soundlibs:jlayer:1.0.1.4")
    shade("org.jcraft:jorbis:0.0.17")

    modCompileOnly("mezz.jei:jei-$jeiVersion:api")
    modRuntimeOnly("mezz.jei:jei-$jeiVersion")
}

tasks {
    processResources {
        project.projectDir.walkTopDown().forEach { file ->
            if (file.name in listOf("mods.toml", "InterfaceLoader.java")) {
                var content = file.readText()
                content = content.replace(Regex("version=\"[^\"]*\""), "version=\"${rootProject.properties["globalVersion"]}\"")
                content = content.replace(Regex("MODVER = \"[^\"]*\";"), "MODVER = \"${rootProject.properties["globalVersion"]}\";")
                file.writeText(content)
            }
        }
    }
    jar {
        dependsOn(project(":mccore").tasks.named("jar"))
        from(shade.files.map { zipTree(it) })

        manifest.attributes(
            "TweakClass" to tweakClass,
            "MixinConfigs" to mixinConfig
        )
    }
    assemble.get().dependsOn(remapJar)
}
