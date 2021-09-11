package org.example.qqgroupadmin.config.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import org.example.qqgroupadmin.QQGroupAdminPluginMain
import org.example.qqgroupadmin.config.ForbiddenKeywords
import org.example.qqgroupadmin.config.MuteData
import org.example.qqgroupadmin.config.PluginConfig

object QQGroupAdminPluginCommand: CompositeCommand(
    QQGroupAdminPluginMain,
    primaryName = "qqgroupadmin",
    description = "QQGroupAdmin主指令",
    secondaryNames = arrayOf("adm", "ga"))
{
    @SubCommand
    @Description("重新加载")
    suspend fun ConsoleCommandSender.reload()
    {
        if(hasPermission(QQGroupAdminPluginMain.permssion_mainCommand))
        {
            QQGroupAdminPluginMain.reloadConf()
            sendMessage("重新加载完成")
            config()
        }
    }

    @SubCommand
    @Description("添加关键词")
    suspend fun CommandSender.kw(action: String="", content: String="")
    {
        when(action)
        {
            "add" -> {
                val cont = content.trim()
                if(cont.isNotEmpty() && cont !in ForbiddenKeywords.key_words)
                {
                    ForbiddenKeywords.key_words.add(cont)
                    sendMessage("添加成功($cont)")
                } else {
                    sendMessage("添加失败($cont)")
                }
            }

            "remove" -> {
                var idx = -1
                try {
                    idx = content.toInt()

                    if(idx >= ForbiddenKeywords.key_words.size)
                    {
                        sendMessage("索引($content)过大，范围:(0 ~ ${ForbiddenKeywords.key_words.size - 1})")
                    } else if (idx < 0) {
                        sendMessage("索引($content)过小，最小只能是0")
                    } else {
                        sendMessage("已移除(${ForbiddenKeywords.key_words[idx]})")
                        ForbiddenKeywords.key_words.removeAt(idx)
                    }

                } catch (e: NumberFormatException) {
                    sendMessage("索引($content)不是一个数字")
                }
            }

            "list" -> {
                var output = ""
                for((k, v) in ForbiddenKeywords.key_words.withIndex())
                {
                    output += "$k: $v\n"
                }
                sendMessage(output)
            }

            else -> {
                sendMessage("未知action($action), 可用值: add <kw>, remove <index>, list")
            }
        }
    }

    @SubCommand
    @Description("显示当前配置")
    suspend fun CommandSender.config()
    {
        var output = ""
        for (vn in PluginConfig.valueNodes)
        {
            output += vn.valueName+": "+vn.value+"\n"
        }
        sendMessage(output)
    }

    @SubCommand
    @Description("显示当前违规信息")
    suspend fun CommandSender.status()
    {
        var output = ""
        for (vn in MuteData.muted)
        {
            output += "${vn.key}: ${vn.value}\n"
        }
        sendMessage(output)
    }

    @SubCommand
    @Description("设置某用户的违规次数")
    suspend fun CommandSender.set(
        @Name("QQ号") qqnumber: Long,
        @Name("违规次数") count: Int)
    {
        MuteData.muted[qqnumber] = count.coerceIn(0, 10000)
        sendMessage("已设置 用户($qqnumber) 为 ${MuteData.muted[qqnumber]}")
    }

}