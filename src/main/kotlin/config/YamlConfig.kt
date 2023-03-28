package com.github.asforest.automute.config

import com.github.asforest.automute.AutoMutePlugin
import org.yaml.snakeyaml.Yaml
import java.io.File

abstract class YamlConfig(val filename: String)
{
    val pluginDataFolder = AutoMutePlugin.configFolder
    val configFile = File(pluginDataFolder, filename)

    fun read(saveDefault: Boolean = false)
    {
        if(saveDefault && !configFile.exists())
            write()

        if(configFile.exists())
        {
            val yaml = Yaml()
            val deserialized = yaml.load<HashMap<String, Any>>(configFile.readText())
            onLoad(deserialized)
        }
    }

    fun write()
    {
        val serialized = LinkedHashMap<String, Any>()
        onSave(serialized)

        val serializedText = Yaml().dumpAsMap(serialized)
        configFile.writeText(serializedText)
    }

    protected abstract fun onLoad(deserialized: HashMap<String, Any>)

    protected abstract fun onSave(serialized: HashMap<String, Any>)
}