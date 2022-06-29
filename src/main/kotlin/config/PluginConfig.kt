package com.github.asforest.automute.config

object PluginConfig: YamlConfig("config.yml")
{
    /**
     * 只报告管理员有人发送违规消息，但不对违规者撤回消息和禁言（调试用）
     */
    var dryRun: Boolean = false

    /**
     * 生效的群聊
     */
    var groupsActivated: MutableList<Long> = mutableListOf()

    /**
     * 谅解次数，用户违规达到这个次数之前仅禁言，达到之后会踢出群聊。设置为0直接踢出
     */
    var toleration: Int = 3

    /**
     * 禁言时长，单位秒。最长30天
     */
    var muteDuration: Int = 24 * 60 * 60

    /**
     * 踢出时的消息
     */
    var kickMessage: String = "你被移除了群聊"

    /**
     * 踢出时是否拉黑（不再接受此人的加群请求）
     */
    var blockWhenKick: Boolean = false

    /**
     * 管理员QQ列表，违规通知会发给所有管理员
     */
    var admins: MutableList<Long> = mutableListOf()

    /**
     * 向管理员报告违规信息时是否将原始消息样本使用Base64编码后发送
     */
    var reportWithBase64Message: Boolean = false

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        dryRun = deserialized["dry-run"] as Boolean
        groupsActivated = deserialized["groups-activated"] as MutableList<Long>
        toleration = deserialized["toleration"] as Int
        muteDuration = deserialized["mute-duration"] as Int
        kickMessage = deserialized["kick-message"] as String
        blockWhenKick = deserialized["block-when-kick"] as Boolean
        admins = deserialized["admins"] as MutableList<Long>
        reportWithBase64Message = deserialized["report-with-base64"] as Boolean
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["dry-run"] = dryRun
        serialized["groups-activated"] = groupsActivated
        serialized["toleration"] = toleration
        serialized["mute-duration"] = muteDuration
        serialized["kick-message"] = kickMessage
        serialized["block-when-kick"] = blockWhenKick
        serialized["admins"] = admins
        serialized["report-with-base64"] = reportWithBase64Message
    }
}