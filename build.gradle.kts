import java.nio.file.Paths
import kotlin.io.path.moveTo

val modVersion: String = project.property("global_version").toString()
val platforms: List<Pair<String, String>> by gradle.extra

tasks.register("buildCore") {
    group = "build"

    val mcCore = project(":mccore")

    dependsOn(mcCore.tasks.getByName("build"))
    doLast {
        moveToOut(mcCore, "core")
    }
}

val buildAllTask = tasks.register("buildForgeAll") registerAll@ {
    group = "build"
}

for((name, moduleName) in platforms) {
    val module = project(moduleName)

    tasks.register("build${ name.replaceFirstChar(Char::titlecase).replace(".", "").replace("-", "") }") {
        group = "build"
        doFirst {
            preBuild()
        }
        doLast {
            moveToOut(module, "1.12.2")
        }
        dependsOn(module.tasks.getByName("build"))
        buildAllTask {
            dependsOn(this@register)
        }
    }
}

fun moveToOut(subProject: Project, versionStr: String) {
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName")
        .moveTo(Paths.get("${project.projectDir.canonicalPath}/out/$jarName"), true)
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