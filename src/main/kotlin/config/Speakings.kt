package com.github.asforest.automute.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Speakings : AutoSavePluginConfig("speakings")
{
    var speakings: MutableMap<Long, Int> by value(mutableMapOf())
}