package com.github.asforest.automute.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig: AutoSavePluginConfig("config")
{
    var enabled: Boolean by value(false)
    var groupsActived: MutableList<Long> by value(mutableListOf())
    var highThreshold: Int by value(3)
    var mediumThreshold: Int by value(10)
    var toleration: Int by value(3)
    var muteDurationInSec: Int by value(24 * 60 * 60)
    var kickMessage: String by value("你被移除了群聊")
    var blockWhenKick: Boolean by value(false)
    var admins: MutableList<Long> by value(mutableListOf())
}