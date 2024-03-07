import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo

var modVersion: String = project.properties["globalVersion"].toString()
version = modVersion

var mcCore = project(":mccore")
var mcInterfaceForge1122 = project(":mcinterfaceforge1122")
var mcInterfaceForge1165 = project(":mcinterfaceforge1165")

plugins {
    // We want java-library because it gives you the normal Java extension
    // but adds library functions like api for dependencies
    id("java-library")
}

// Anything in this closure is applied to all projects
// Note: This is not called in time to apply the above java-library plugin
allprojects {
    repositories {
        mavenCentral()
    }
}

// Anything in this closure is applied to all subprojects
subprojects {
    apply(plugin = "java-library")

    // Set the version for all subprojects
    version = if (project.name == "mccore") {
        "Core-${rootProject.properties["globalVersion"]}"
    } else {
        "${project.properties["mcVersion"]}-${rootProject.properties["globalVersion"]}"
    }

    // Don't apply any of this to core
    if (project.name != "mccore") {
        sourceSets.main {
            output.resourcesDir = file("$buildDir/classes/java/main")
        }

        repositories {
            maven("https://dvs1.progwml6.com/files/maven/") // JEI
        }
    }

    java {
        withSourcesJar()
    }

    tasks {
        // Set compiler properties
        withType<JavaCompile> {
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
            options.encoding = "UTF-8"
            options.release.set(8)
            options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
        }

        // Make the jars have the correct name
        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.WARN
            archiveBaseName.set(rootProject.name)
        }
    }
}

// MTS build tasks
tasks {
    val buildCore = register("buildCore") {
        group = "mts"
        dependsOn(mcCore.tasks.getByName("build"))
    }

    val buildForge1122 = register<JavaCompile>("buildForge1122") {
        group = "mts"
        doLast { moveToOut(mcInterfaceForge1122) }
        dependsOn(buildCore, mcInterfaceForge1122.tasks.getByName("build"))
    }

    val buildForge1165 = register<JavaCompile>("buildForge1165") {
        group = "mts"
        doLast { moveToOut(mcInterfaceForge1165) }
        dependsOn(buildCore, mcInterfaceForge1165.tasks.getByName("build"))
    }

    register<JavaCompile>("buildForgeAll") {
        group = "mts"
        dependsOn(buildCore, buildForge1122, buildForge1165)
    }
}

val outPath: Path = Paths.get("${project.projectDir.canonicalPath}/out/")

// Puts all jars into an easily accessible directory
fun moveToOut(subProject: Project) {
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    if (!outPath.exists()) outPath.createDirectories()
    println(outPath.exists())
    Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName")
        .moveTo(Paths.get(outPath.toString(), jarName), true)
}
