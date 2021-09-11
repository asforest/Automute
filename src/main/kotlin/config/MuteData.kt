package org.example.qqgroupadmin.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MuteData: AutoSavePluginConfig("mute-data")
{
    var muted: MutableMap<Long, Int> by value(mutableMapOf())
}