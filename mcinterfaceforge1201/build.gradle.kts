@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.architectury.loom") version "1.6-SNAPSHOT"
}

val mc_version: String by project
val forge_version: String by project

val mod_version: String by project
val mod_group: String by project
val archive_name: String by project
val mod_id: String by project

base.archivesName.set(archive_name)
version = "${mc_version}-${mod_version}"
group = mod_group

val generatedResources = file("src/generated")

loom {
    silentMojangMappingsLicense()

    forge {
        convertAccessWideners = true
        mixinConfig("$mod_id.mixins.json")
    }

    runs {
        named("client") {
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            ideConfigGenerated(true)
            runDir("runServer")
        }
    }
}

val embed: Configuration by configurations.creating

configurations.implementation {
    extendsFrom(embed)
}

sourceSets {
    main {
        resources.srcDir(generatedResources)
    }
}

repositories {
    mavenCentral()
    maven {
        name = "ParchmentMC"
        setUrl("https://maven.parchmentmc.org")
    }
    maven {
        name = "BlameJared"
        setUrl("https://maven.blamejared.com")
        content {
            includeGroup("mezz.jei")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$mc_version")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.20.1:2023.09.03@zip")
    })
    forge("net.minecraftforge:forge:$forge_version")

    "embed"(project(":mccore"))

    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.6")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.6")
    implementation("io.github.llamalad7:mixinextras-forge:0.3.6")
    include("io.github.llamalad7:mixinextras-forge:0.3.6")

    modCompileOnlyApi("mezz.jei:jei-1.20.1-common-api:15.3.0.8")
    modCompileOnlyApi("mezz.jei:jei-1.20.1-forge-api:15.3.0.8")
    modRuntimeOnly("mezz.jei:jei-1.20.1-forge:15.3.0.8")
}

tasks.jar {
    from(embed.map { if(it.isDirectory) it else zipTree(it) })
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(arrayOf("-Xlint:deprecation", "-Xlint:unchecked"))
    options.encoding = "UTF-8"
}