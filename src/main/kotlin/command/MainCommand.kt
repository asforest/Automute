package com.github.asforest.automute.command

import com.github.asforest.automute.AutoMutePlugin
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.PluginConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import java.util.*

object MainCommand: CompositeCommand(
    AutoMutePlugin,
    primaryName = "automute",
    description = "AutoMute主指令",
    secondaryNames = arrayOf("am", "adm"),
    parentPermission = AutoMutePlugin.permssion_mainCommand
) {
    @SubCommand
    @Description("重新加载")
    suspend fun ConsoleCommandSender.reload()
    {
        AutoMutePlugin.reloadConf()
        sendMessage("重新加载完成")
    }

    @SubCommand
    @Description("显示当前配置文件和关键字列表")
    suspend fun CommandSender.info()
    {
        val c = PluginConfig
        val output =
                "插件无实际动作: ${c.dryRun}\n" +
                "生效的群聊列表：${c.groupsActivated}\n" +
                "踢出前禁言次数：${c.toleration}\n" +
                "禁言时长单位秒：${c.muteDuration}\n" +
                "踢出时附加消息：${c.kickMessage}\n" +
                "踢出时是否拉黑：${c.blockWhenKick}\n" +
                "管理员名单列表：${c.admins}\n" +
                "编码消息样本：${!c.reportWithBase64Message}\n" +
                "报告模板：\n====================\n${c.reportTemplate}\n====================\n"

        val output2 = Keywords.restrictionLevels.withIndex().joinToString("\n\n") {
            "规则${it.index}（检测发言数量：${it.value.level}）：\n" +
                    it.value.keywords.joinToString("\n") { i -> "- $i" }
        }

        sendMessage(output + output2)
    }

    @SubCommand
    @Description("解码一个base64消息样本(utf8)")
    suspend fun CommandSender.decode(b64: String)
    {
        try {
            val decoded = Base64.getDecoder().decode(b64.trim()).decodeToString()
            sendMessage(decoded)
        } catch (e: IllegalArgumentException) {
            sendMessage("消息不是b64格式，解码失败")
        }
    }
}