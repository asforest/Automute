import java.util.Date
import java.text.SimpleDateFormat

val gitTagName: String? get() = Regex("(?<=refs/tags/).*").find(System.getenv("GITHUB_REF") ?: "")?.value
val gitCommitSha: String? get() = System.getenv("GITHUB_SHA") ?: null
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(Date()) as String

group = "com.github.asforest"
version = gitTagName ?: "0.4"

plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.7.0"
}

repositories {
//    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

tasks.register("buildWithManifest") {
    dependsOn(tasks.named("buildPlugin"))

    tasks.named<net.mamoe.mirai.console.gradle.BuildMiraiPluginTask>("buildPlugin").get().apply {
        manifest {
            attributes("Mirai-Plugin-Id" to "$group.automute")
            attributes("Mirai-Plugin-Name" to "AutoMute")
            attributes("Mirai-Plugin-Version" to archiveVersion.get())
            attributes("Mirai-Plugin-Author" to "Asforest")
            attributes("Git-Commit" to (gitCommitSha ?: ""))
            attributes("Compile-Time" to timestamp)
            attributes("Compile-Time-Ms" to System.currentTimeMillis())
        }
    }
}

tasks.register("developing", Copy::class) {
    dependsOn(tasks.named("buildWithManifest"))

    val archive = project.buildDir.path+File.separator+"mirai"+
            File.separator+project.name+"-"+version+".mirai.jar"

    val env = System.getenv()["PluginDebugDir"]
        ?: throw RuntimeException("The environmental variable 'PluginDebugDir' is not set")
    if(!File(env).run { !exists() || isDirectory })
        throw RuntimeException("The 'PluginDebugDir' $env does not exist or is a file")

    from(archive).into(env)
}