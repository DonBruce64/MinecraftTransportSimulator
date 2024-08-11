plugins {
    id("io.github.goooler.shadow") version "8.1.7"
}

configurations.implementation {
    extendsFrom(configurations.getByName("shadow"))
}

dependencies {
    shadow("com.googlecode.soundlibs:jlayer:1.0.1.4")
    shadow("org.jcraft:jorbis:0.0.17")

    compileOnly("com.google.code.gson:gson:2.8.0") // 1.12.2: 2.8.0
    compileOnly("io.netty:netty-all:4.1.9.Final") // 1.12.2: 4.1.9.Final
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
