import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo


plugins {
}

repositories {
}

dependencies {
}

subprojects {
    apply(plugin = "java")
}

var modVersion: String = project.properties["global_version"].toString()

var mcCore = project(":mccore")
var mcInterfaceForge1122 = project(":mcinterfaceforge1122")
var mcInterfaceForge1165 = project(":mcinterfaceforge1165")

tasks.register("buildCore") {
    group = "mts"
    dependsOn(mcCore.tasks.getByName("build"))
}

tasks.register<JavaCompile>("buildForge1122") {
    group = "mts"
    doLast { moveToOut(mcInterfaceForge1122) }
    dependsOn(mcInterfaceForge1122.tasks.getByName("build"))
}

tasks.register<JavaCompile>("buildForge1165") {
    group = "mts"
    doLast { moveToOut(mcInterfaceForge1165) }
    dependsOn(mcInterfaceForge1165.tasks.getByName("build"))
}

tasks.register<JavaCompile>("buildForgeAll") {
    group = "mts"
    dependsOn(tasks.getByName("buildForge1122"))
    dependsOn(tasks.getByName("buildForge1165"))
}

@OptIn(ExperimentalPathApi::class)
fun moveToOut(subProject: Project) {
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    val outPath = Paths.get("${project.projectDir.canonicalPath}/out/")
    if (!outPath.exists()) outPath.createDirectories()
    Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName").moveTo(Paths.get(outPath.toString(), jarName), true)
}