package com.github.asforest.automute.command

import com.github.asforest.automute.AutoMutePlugin
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.PluginConfig
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender

object AutoMuteMainCommand: CompositeCommand(
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
        val output = "仅通知管理: ${c.dryRun}\n" +
                "生效的群聊：${c.groupsActivated}\n" +
                "总谅解次数：${c.toleration}\n" +
                "禁言时长秒：${c.muteDuration}\n" +
                "踢出时消息：${c.kickMessage}\n" +
                "踢出时拉黑：${c.blockWhenKick}\n" +
                "管理员列表：${c.admins}\n\n"

        val output2 = Keywords.restrictionLevels.withIndex().joinToString("\n\n") {
            "规则${it.index}（检测发言数量：${it.value.level}）：\n" +
                    it.value.keywords.joinToString("\n") { i -> "- $i" }
        }

        sendMessage(output + output2)
    }
}