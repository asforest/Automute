package com.github.asforest.automute.config

object Keywords: YamlConfig("keywords.yml")
{
    val restrictionLevels: MutableList<RestrictionLevel> = mutableListOf()

    /**
     * 测试发言是否违规
     * @param text 发言的文字
     * @param speakings 是第一次发言
     */
    fun isSpeakingViolated(text: String, speakings: Int): ViolationEvent?
    {
        return restrictionLevels.firstNotNullOfOrNull {
            if(speakings < it.level) Pair(it, it.testKeywords(text)) else null
        }?.run { second?.let { ViolationEvent(it, first) } }
    }

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        restrictionLevels.clear()

        for (key in deserialized.keys.mapNotNull { it.toIntOrNull() })
        {
            restrictionLevels.add(RestrictionLevel(
                level = key,
                keywords = deserialized[key.toString()] as MutableList<String>
            ))
        }
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        for (rl in restrictionLevels)
            serialized[rl.level.toString()] = rl.keywords
    }

    /**
     * 违规检测等级
     * @param level 从第几次发言时开始会进行检测
     * @param keywords 要检测的关键字
     */
    data class RestrictionLevel(
        val level: Int,
        val keywords: MutableList<String>
    ) {
        fun testKeywords(text: String): String? {
            return keywords.firstOrNull {
                Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(text)
            }
        }
    }

    data class ViolationEvent(
        val keyword: String,
        val restrictionLevel: RestrictionLevel,
    )
}
