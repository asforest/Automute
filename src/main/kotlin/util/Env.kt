package com.github.asforest.automute.util

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import java.io.File
import java.net.URLDecoder
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object Env
{
    private val MF: Map<String, String> = manifest

    val version: String get() = MF["Mirai-Plugin-Version"] ?: "0.0.0"

    val gitCommit: String get() = MF["Git-Commit"] ?: "<development>"

    val compileTime: String get() = MF["Compile-Time"] ?: "<no compile time>"

    val compileTimeMs: Long get() = MF["Compile-Time-Ms"]?.toLong() ?: 0L

    val pluginDescription: JvmPluginDescription get() = JvmPluginDescription("com.github.asforest.automute", version, "Automute")

    /**
     * 读取版本信息（程序打包成Jar后才有效）
     * @return Application版本号，如果为打包成Jar则返回null
     */
    val manifest: Map<String, String> get()
    {
        return originManifest?.entries?.associate { it.key.toString() to it.value.toString() } ?: mapOf()
    }

    val originManifest: Attributes?
        get()
        {
            if(!isPackaged)
                return null

            JarFile(jarFile.path).use { jar ->
                jar.getInputStream(jar.getJarEntry("META-INF/MANIFEST.MF")).use {
                    return Manifest(it).mainAttributes
                }
            }
        }

    /**
     * 程序是否被打包
     */
    @JvmStatic
    val isPackaged: Boolean get() = javaClass.getResource("").protocol != "file"

    /**
     * 获取当前Jar文件路径（仅打包后有效）
     */
    @JvmStatic
    val jarFile: File get() = File(URLDecoder.decode(Env.javaClass.protectionDomain.codeSource.location.file, "UTF-8"))

}