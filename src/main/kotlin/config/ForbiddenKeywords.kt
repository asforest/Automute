package org.example.qqgroupadmin.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object ForbiddenKeywords: AutoSavePluginConfig("forbidden_keywords")
{
    var key_words: MutableList<String> by value(mutableListOf())
}
