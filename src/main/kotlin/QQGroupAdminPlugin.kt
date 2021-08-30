package org.example.qqgroupadmin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import org.example.qqgroupadmin.PluginConf.provideDelegate
import java.util.*

object QQGroupAdminPlugin : KotlinPlugin(
    JvmPluginDescription.loadFromResource()
) {
    val PERMISSION_COMMAND by lazy { PermissionService.INSTANCE.register(permissionId("qqgroupadmin"), "QQGroupAdmin主指令") }

    override fun onEnable()
    {
        reloadConf()

        // 注册权限
        PERMISSION_COMMAND

        // 注册指令
        QQGroupAdminPluginCommand.register()

        var channel = GlobalEventChannel.filter { it is BotEvent }
        channel.subscribeAlways<GroupMessageEvent> {
            if(group.id !in PluginConf.groupsActived)
                return@subscribeAlways

            if(bot.getGroup(group.id)!!.botPermission == MemberPermission.MEMBER)
                return@subscribeAlways

            // 只对普通群成员生效(管理员和群主无效)
            if(sender is NormalMember && sender.permission == MemberPermission.MEMBER)
            {
                val member = sender as NormalMember

                for (kw in ForbiddenKeywords.key_words)
                {
                    val qq = member.id
                    val nick = member.nick
                    val msg = message.content

                    // 如果触发敏感词
                    if (msg.lowercase(Locale.getDefault()).contains(kw))
                    {
//                        logger.info("$nick($qq) 触发了禁止关键字($kw): $msg")
                        logger.warning("$nick($qq) 触发了禁止关键字($kw)")

                        if(qq !in MuteData.muted)
                            MuteData.muted[qq] = 0

                        val timesMuted = MuteData.muted[qq]!!
                        MuteData.muted[qq] = timesMuted + 1
                        val kick = timesMuted >= PluginConf.toleration

                        // 转发样本数据
                        val times = MuteData.muted[qq]
                        val timesMax = PluginConf.toleration

                        val msg0 = message.toForwardMessage(bot.id, bot.nick)
                        val msg1 = "$nick($qq)触发关键字($kw)($times/$timesMax)"+(if (kick) "(已踢)" else "")

                        for (adm in PluginConf.admins)
                        {
                            val to = bot.getFriend(adm)
                            to?.sendMessage(msg0)
                            to?.sendMessage(msg1)
                        }

                        // 实际动作
                        if(PluginConf.enabled)
                        {
                            message.recall()

                            if(kick)
                            {
                                member.kick(PluginConf.kickMessage, PluginConf.blockWhenKick)
                            } else {
                                member.mute(PluginConf.muteDurationInSec)
                            }
                        }

                        break
                    }
                }
            }
        }
    }

    fun reloadConf()
    {
        PluginConf.reload()
        MuteData.reload()
        ForbiddenKeywords.reload()

        logger.info("Reloaded!")
    }

}

object PluginConf: AutoSavePluginConfig("config")
{
    var enabled: Boolean by value(false)
    var groupsActived: MutableList<Long> by value(mutableListOf())
    var toleration: Int by value(3)
    var muteDurationInSec: Int by value(24 * 60 * 60)
    var kickMessage: String by value("你被移除了群聊")
    var blockWhenKick: Boolean by value(false)
    var admins: MutableList<Long> by value(mutableListOf())
}

object MuteData: AutoSavePluginConfig("mute-data")
{
    var muted: MutableMap<Long, Int> by value(mutableMapOf())
}

object ForbiddenKeywords: AutoSavePluginConfig("forbidden_keywords")
{
    var key_words: MutableList<String> by value(mutableListOf())
}

object QQGroupAdminPluginCommand: CompositeCommand(
    QQGroupAdminPlugin,
    primaryName = "qqgroupadmin",
    description = "QQGroupAdmin主指令",
    secondaryNames = arrayOf("adm", "ga"))
{
    @SubCommand
    @Description("重新加载")
    suspend fun ConsoleCommandSender.reload()
    {
        if(hasPermission(QQGroupAdminPlugin.PERMISSION_COMMAND))
        {
            QQGroupAdminPlugin.reloadConf()
            sendMessage("重新加载完成")
            config()
        }
    }

    @SubCommand
    @Description("添加关键词")
    suspend fun CommandSender.kw(action: String, content: String ="")
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
                sendMessage("未知action($action)")
            }
        }
    }

    @SubCommand
    @Description("显示当前配置")
    suspend fun CommandSender.config()
    {
        var output = ""
        for (vn in PluginConf.valueNodes)
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