package com.github.asforest.automute

import com.github.asforest.automute.command.MainCommand
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.MuteRecordConfig
import com.github.asforest.automute.config.PluginConfig
import com.github.asforest.automute.config.SpeakingsConfig
import com.github.asforest.automute.util.Env
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
import java.util.*

object AutoMutePlugin : KotlinPlugin(Env.pluginDescription)
{
    val permssion_mainCommand by lazy { PermissionService.INSTANCE.register(AutoMutePlugin.permissionId("all"), "AutoMutePlugin主指令") }

    override fun onEnable()
    {
        reloadConf()

        // 注册权限
        permssion_mainCommand

        // 注册指令
        MainCommand.register()

        GlobalEventChannel.filter { it is BotEvent }.subscribeAlways<GroupMessageEvent> {
            if(!PluginConfig.groupsActivated.any { it == group.id })
                return@subscribeAlways

            if(bot.getGroup(group.id)!!.botPermission == MemberPermission.MEMBER)
                return@subscribeAlways

            val qq = sender.id
            val nick = sender.nick
            val msg = message.content
            val speakings = SpeakingsConfig.getSpeakings(qq) // 是第几次发言

            // 测试是否违规
            val isViolated = Keywords.isSpeakingViolated(msg, speakings)

            // 没有违规则发言次数+1
            if (isViolated == null)
            {
                SpeakingsConfig.addSpeakings(qq)
                return@subscribeAlways
            }

            // 只对普通群成员生效(管理员和群主无效)
            if(sender.permission != MemberPermission.MEMBER)
                return@subscribeAlways

            // 曾经的违规次数
            val violations = MuteRecordConfig.getMutedCount(qq)

            // 违规次数+1
            MuteRecordConfig.addMutedCount(qq)

            // 已经没有改过自新的机会了
            val isOutOfToleration = violations >= PluginConfig.toleration

            // 构建管理员报告消息
            val sample = if (PluginConfig.reportWithBase64Message)
                Base64.getEncoder().encodeToString(msg.encodeToByteArray())
            else
                msg

            // 检查机器人是否有管理权限
            val botPermission = this.group.botPermission;
            val hasProvilege = botPermission == MemberPermission.OWNER || botPermission == MemberPermission.ADMINISTRATOR

            val report = buildString {
                appendLine("检测到 $nick($qq)")
                appendLine("在QQ群聊 ${group.name}(${group.id}) 的发言")
                appendLine("违反了关键字【${isViolated.keyword}】")
                appendLine("这是第${violations}次，当达到第${PluginConfig.toleration}时会被请出群聊")

                val actions = mutableListOf<String>()

                if (hasProvilege)
                {
                    actions.add("撤回消息")
                    if (isOutOfToleration)
                        actions.add("请出群聊")
                    else
                        actions.add("已禁言")
                } else {
                    actions.add("没有群聊管理权限")
                }

                appendLine("当前动作：${actions.joinToString(" + ")}")
                append("以下是")
                append(if (PluginConfig.reportWithBase64Message) "base64编码后的" else "原始")
                appendLine("消息：")
                appendLine(sample)
            }

            // 向所有管理员报告
            for (adm in PluginConfig.admins) {
                val to = bot.getFriend(adm)
                to?.sendMessage(report)
            }

            // 实际动作：撤回消息+禁言或者踢出
            if (!PluginConfig.dryRun && hasProvilege) {
                message.recall()

                if (isOutOfToleration) {
                    (sender as NormalMember).kick(PluginConfig.kickMessage, PluginConfig.blockWhenKick)
                } else {
                    (sender as NormalMember).mute(PluginConfig.muteDuration)
                }
            }

        }
    }

    fun reloadConf()
    {
        PluginConfig.read(saveDefault = true)
        MuteRecordConfig.reload()
        Keywords.read(saveDefault = true)
        SpeakingsConfig.reload()

        logger.info("Reloaded!")
    }
}