package com.github.asforest.automute.config

import com.github.asforest.automute.AutoMutePlugin.save
import com.github.asforest.automute.util.Env
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MuteRecordConfig: AutoSavePluginConfig("mute-data")
{
    private var muted: MutableMap<Long, Int> by value(mutableMapOf())

    /**
     * 获取曾经惯犯次数
     */
    fun getMutedCount(qq: Long): Int {
        return muted.getOrDefault(qq, 0)
    }

    /**
     * 惯犯次数+1
     */
    fun addMutedCount(qq: Long) {
        muted[qq] = getMutedCount(qq) + 1

        if (Env.inDev)
            save()
    }
}