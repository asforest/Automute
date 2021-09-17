package com.github.asforest.automute.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object Keywords: AutoSavePluginConfig("keywords")
{
    var high: MutableList<String> by value(mutableListOf())
    var medium: MutableList<String> by value(mutableListOf())
    var low: MutableList<String> by value(mutableListOf())
}
