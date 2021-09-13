package org.example.qqgroupadmin

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import okio.ByteString.Companion.encode
import org.example.qqgroupadmin.config.ForbiddenKeywords
import org.example.qqgroupadmin.config.MuteData
import org.example.qqgroupadmin.config.PluginConfig
import org.example.qqgroupadmin.command.QQGroupAdminPluginCommand
import java.nio.charset.Charset
import java.util.*

object QQGroupAdminPluginMain : KotlinPlugin(JvmPluginDescription.loadFromResource())
{
    val permssion_mainCommand by lazy { PermissionService.INSTANCE.register(permissionId("qqgroupadmin"), "QQGroupAdmin主指令") }

    override fun onEnable()
    {
        reloadConf()

        // 注册权限
        permssion_mainCommand

        // 注册指令
        QQGroupAdminPluginCommand.register()

        GlobalEventChannel.filter { it is BotEvent }.subscribeAlways<GroupMessageEvent> {
            if(group.id !in PluginConfig.groupsActived)
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
                    if (Regex(kw, RegexOption.IGNORE_CASE).containsMatchIn(msg))
                    {
                        logger.warning("$nick($qq) 触发关键字($kw)")
                        logger.warning("以下是原消息: $msg")

                        if(qq !in MuteData.muted)
                            MuteData.muted[qq] = 0

                        val timesMuted = MuteData.muted[qq]!!
                        MuteData.muted[qq] = timesMuted + 1
                        val kick = timesMuted >= PluginConfig.toleration

                        // 转发样本数据
                        val times = MuteData.muted[qq]
                        val timesMax = PluginConfig.toleration

//                        val msg0 = message.toForwardMessage(bot.id, bot.nick)
                        val msg1 = "$nick($qq)触发关键字($kw)($times/$timesMax)"+(if (kick) "(已踢)" else "")
                        val contentInB64 = Base64.getEncoder().encodeToString(msg.encode(Charset.forName("utf-8")).toByteArray())

                        for (adm in PluginConfig.admins)
                        {
                            val to = bot.getFriend(adm)
                            to?.sendMessage(msg1+"\n"+contentInB64)
                        }

                        // 实际动作
                        if(PluginConfig.enabled)
                        {
                            message.recall()

                            if(kick)
                            {
                                member.kick(PluginConfig.kickMessage, PluginConfig.blockWhenKick)
                            } else {
                                member.mute(PluginConfig.muteDurationInSec)
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
        PluginConfig.reload()
        MuteData.reload()
        ForbiddenKeywords.reload()

        logger.info("Reloaded!")
    }

}