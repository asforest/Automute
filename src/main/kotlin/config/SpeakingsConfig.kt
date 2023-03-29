package com.github.asforest.automute.config

import com.github.asforest.automute.AutoMutePlugin.save
import com.github.asforest.automute.util.Env
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object SpeakingsConfig : AutoSavePluginConfig("speakings")
{
    var speakings: MutableMap<Long, Int> by value(mutableMapOf())

    fun getSpeakings(qq: Long): Int
    {
        return speakings.getOrDefault(qq, 0)
    }

    fun addSpeakings(qq: Long)
    {
        speakings[qq] = getSpeakings(qq) + 1

        if (Env.inDev)
            save()
    }
}