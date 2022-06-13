package com.github.asforest.automute

import com.github.asforest.automute.command.AutoMuteMainCommand
import com.github.asforest.automute.config.Keywords
import com.github.asforest.automute.config.MuteRecordConfig
import com.github.asforest.automute.config.PluginConfig
import com.github.asforest.automute.config.SpeakingsConfig
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
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

object AutoMutePlugin : KotlinPlugin(JvmPluginDescription(
    id = "com.github.asforest.automute",
    version = "1.0.0",
    name = "AutoMute",
)) {
    val permssion_mainCommand by lazy { PermissionService.INSTANCE.register(AutoMutePlugin.permissionId("all"), "AutoMutePlugin主指令") }

    override fun onEnable()
    {
        reloadConf()

        // 注册权限
        permssion_mainCommand

        // 注册指令
        AutoMuteMainCommand.register()

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
            val rawMessageContentBase64 = Base64.getEncoder()
                .encodeToString(msg.encode(Charset.forName("utf-8")).toByteArray())
            val reportMessage = "检测到 $nick($qq) 在QQ群聊 ${group.name}(${group.id}) 的发言违反了关键字【${isViolated.keyword}】，" +
                    "次数($violations/${PluginConfig.toleration})" +
                    (if (isOutOfToleration) "(已踢出群聊)" else "（已禁言）") +
                    "\n以下原始消息(base64)：\n$rawMessageContentBase64"

            // 向所有管理员报告
            for (adm in PluginConfig.admins) {
                val to = bot.getFriend(adm)
                to?.sendMessage(reportMessage)
            }

            // 实际动作：撤回消息+禁言或者踢出
            if (!PluginConfig.dryRun) {
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