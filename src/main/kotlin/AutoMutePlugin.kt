package com.github.asforest.automute

import com.github.asforest.automute.command.AutoMuteMainCommand
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.MainConfig
import com.github.asforest.automute.config.MuteData
import com.github.asforest.automute.config.Speakings
import com.github.asforest.automute.exception.InterruptCheckException
import com.github.asforest.automute.util.MiraiUtil
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.content
import okio.ByteString.Companion.encode
import java.nio.charset.Charset
import java.util.*

object AutoMutePlugin : KotlinPlugin(MiraiUtil.pluginDescription)
{
    val permssion_mainCommand by lazy { PermissionService.INSTANCE.register(AutoMutePlugin.permissionId("all1"), "AutoMutePlugin主指令") }

    override fun onEnable()
    {
        reloadConf()

        // 注册权限
        permssion_mainCommand

        // 注册指令
        AutoMuteMainCommand.register()

        GlobalEventChannel.filter { it is BotEvent }.subscribeAlways<GroupMessageEvent> {
            if(group.id !in MainConfig.groupsActived)
                return@subscribeAlways

            if(bot.getGroup(group.id)!!.botPermission == MemberPermission.MEMBER)
                return@subscribeAlways

            // 只对普通群成员生效(管理员和群主无效)
            if(sender is NormalMember && sender.permission == MemberPermission.MEMBER)
            {
                val member = sender as NormalMember
                val qq = member.id
                val nick = member.nick
                val msg = message.content
                val speakings = if(qq in Speakings.speakings.keys) Speakings.speakings[qq]!! else 1

                Speakings.speakings[qq] = speakings + 1

                suspend fun q(cb: suspend (p: MutableList<String>, prio: String) -> Unit)
                {
                    try {
                        if(speakings <= MainConfig.highThreshold)
                            cb(Keywords.high, "high")
                        if (speakings <= MainConfig.mediumThreshold)
                             cb(Keywords.medium, "medium")
                        cb(Keywords.low, "low")
                    } catch (e: InterruptCheckException) {}
                }

                q { p, prio ->
                    for (kw in p)
                    {
                        // 如果触发敏感词
                        if (Regex(kw, RegexOption.IGNORE_CASE).containsMatchIn(msg))
                        {
                            logger.warning("$nick($qq) 触发关键字($kw)")
                            logger.warning("以下是原消息: $msg")

                            if(qq !in MuteData.muted)
                                MuteData.muted[qq] = 0

                            val timesMuted = MuteData.muted[qq]!!
                            MuteData.muted[qq] = timesMuted + 1
                            val kick = timesMuted >= MainConfig.toleration

                            // 转发样本数据
                            val times = MuteData.muted[qq]
                            val timesMax = MainConfig.toleration

//                        val msg0 = message.toForwardMessage(bot.id, bot.nick)
                            val msg1 = "$nick($qq)触发关键字($kw)($times/$timesMax)($prio)"+(if (kick) "(已踢)" else "")
                            val contentInB64 = Base64.getEncoder().encodeToString(msg.encode(Charset.forName("utf-8")).toByteArray())

                            for (adm in MainConfig.admins)
                            {
                                val to = bot.getFriend(adm)
                                to?.sendMessage(msg1+"\n"+contentInB64)
                            }

                            // 实际动作
                            if(MainConfig.enabled)
                            {
                                message.recall()

                                if(kick)
                                {
                                    member.kick(MainConfig.kickMessage, MainConfig.blockWhenKick)
                                } else {
                                    member.mute(MainConfig.muteDurationInSec)
                                }
                            }

                            throw InterruptCheckException()
                        }
                    }
                }


            }
        }
    }

    fun reloadConf()
    {
        MainConfig.reload()
        MuteData.reload()
        Keywords.reload()
        Speakings.reload()

        logger.info("Reloaded!")
    }

}