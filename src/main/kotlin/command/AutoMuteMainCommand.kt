package com.github.asforest.automute.command

import com.github.asforest.automute.AutoMutePlugin
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.MainConfig
import com.github.asforest.automute.config.MuteData
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
        config()
    }

    @SubCommand
    @Description("添加关键词")
    suspend fun CommandSender.kw(action: String ="", priority: String ="", content: String ="")
    {
        if(action != "add" && action != "remove" && action != "list") {
            sendMessage("未知action($action), 可用值: add <priority> <kw>, remove <index>, list")
            return
        }

        if(priority!="high" && priority!="medium" && priority!="low")
        {
            sendMessage("找不到优先级: $priority, 可用值: high, medium, low")
            return
        }

        suspend fun q(cb: suspend (p: MutableList<String>) -> Unit)
        {
            if(priority == "high")
            {
                cb(Keywords.high)
            } else if (priority == "medium") {
                cb(Keywords.medium)
            } else if (priority == "low") {
                cb(Keywords.low)
            }
        }

        when(action)
        {
            "add" -> {
                val _content = content.trim()

                if(_content.isEmpty())
                {
                    sendMessage("content不能为空")
                    return
                }

                q {
                    if(_content in it)
                        Keywords.high.remove(_content)
                    it += _content
                    sendMessage("添加成功($_content)")
                }
            }

            "remove" -> {
                q {
                    var idx = -1
                    try {
                        idx = content.toInt()

                        if(idx >= it.size)
                        {
                            sendMessage("索引($content)过大，范围:(0 ~ ${it.size - 1})")
                        } else if (idx < 0) {
                            sendMessage("索引($content)过小，最小只能是0")
                        } else {
                            sendMessage("已移除(${it[idx]})")
                            it.removeAt(idx)
                        }

                    } catch (e: NumberFormatException) {
                        sendMessage("索引($content)不是一个数字")
                    }
                }
            }

            "list" -> {
                q {
                    var output = ""
                    for((k, v) in it.withIndex())
                    {
                        output += "$k: $v\n"
                    }
                    sendMessage(output)
                }
            }
        }
    }

    @SubCommand
    @Description("显示所有关键字")
    suspend fun CommandSender.kwlist()
    {
        var output = ""
        suspend fun q(cb: suspend (p: MutableList<String>) -> Unit)
        {
            output += "-----High-----\n"
            cb(Keywords.high)
            output += "-----Medium-----\n"
            cb(Keywords.medium)
            output += "-----Low-----\n"
            cb(Keywords.low)
        }
        q {
            for((k, v) in it.withIndex())
                output += "$k: $v\n"
        }
        sendMessage(output)
    }

    @SubCommand
    @Description("显示当前配置")
    suspend fun CommandSender.config()
    {
        var output = ""
        for (vn in MainConfig.valueNodes)
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