import java.util.Date
import java.text.SimpleDateFormat
import net.mamoe.mirai.console.gradle.BuildMiraiPluginV2

val gitTagName: String? get() = Regex("(?<=refs/tags/).*").find(System.getenv("GITHUB_REF") ?: "")?.value
val gitCommitSha: String? get() = System.getenv("GITHUB_SHA") ?: null
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(Date()) as String

group = "com.github.asforest"
version = gitTagName ?: "0.0.0"

plugins {
    val kotlinVersion = "1.6.10"
    val miraiVersion = "2.11.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version miraiVersion
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:1.30")
}

afterEvaluate {
    tasks.named<BuildMiraiPluginV2>("buildPlugin") {
        // 在manifest里添加信息
        manifest {
            attributes("Mirai-Plugin-Version" to archiveVersion.get())
            attributes("Git-Commit" to (gitCommitSha ?: ""))
            attributes("Compile-Time" to timestamp)
        }
    }
}

tasks.register<Copy>("develop") {
    val buildMiraiPluginTask = tasks.named<BuildMiraiPluginV2>("buildPlugin")
    dependsOn(buildMiraiPluginTask)

    val archive = buildMiraiPluginTask.get().archiveFile.get().asFile
    val outputPath = System.getenv()["DBG"]?.replace("/", "\\")
    val outputDir = outputPath?.run { File(this) }
        ?.run { if(!exists() || !isDirectory) null else this }

    if(outputDir != null)
    {
        from(archive).into(outputPath)
        println("Copy $archive -> $outputPath")
    }
}