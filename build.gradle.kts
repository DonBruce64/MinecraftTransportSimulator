import java.nio.file.Paths
import kotlin.io.path.moveTo
import kotlin.io.path.ExperimentalPathApi


plugins {
    java
    kotlin("jvm") version "1.7.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

subprojects {
    apply(plugin = "java")
}

var modVersion: String = "22.6.0-BETA"

var mcCore = project(":mccore")
var mcInterfaceForge1122 = project(":mcinterfaceforge1122")
var mcInterfaceForge1165 = project(":mcinterfaceforge1165")

tasks.register("buildCore") {
    dependsOn(mcCore.tasks.build)
    doLast {
        moveToOut(mcCore, "core")
    }
}

tasks.register("buildForge1.12.2") {
    doFirst { preBuild() }
    dependsOn(mcInterfaceForge1122.tasks.build)
    doLast {
        moveToOut(mcInterfaceForge1122, "1.12.2")
        moveToOut(mcCore, "core")
    }
}

tasks.register("buildForge1.16.5") {
    doFirst { preBuild() }
    doLast {
        moveToOut(mcInterfaceForge1165, "1.16.5")
        moveToOut(mcCore, "core")
    }
    dependsOn(mcInterfaceForge1165.tasks.build)
}

@OptIn(ExperimentalPathApi::class)
fun moveToOut(subProject: Project, versionStr: String) {
    mkdir("out/$versionStr")
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName")
        .moveTo(Paths.get("${project.projectDir.canonicalPath}/out/$versionStr/$jarName"), true)
}

fun preBuild() {
    // Could probably be better somehow, but I'm not sure how
    project.projectDir.canonicalFile.walk()
        .filter { it.name == "gradle.properties" || it.name == "mcmod.info" || it.name == "InterfaceLoader.java" }
        .forEach { it.writeText(it.readText()
            .replace(Regex("mod_version=(.+)"), "mod_version=$modVersion")
            .replace(Regex("\"version\": \"[^\"]*\""), "\"version\": \"$modVersion\"")
            .replace(Regex("MODVER = \"[^\"]*\";"), "MODVER = \"$modVersion\";")) }
}