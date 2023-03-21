package com.mjb2k.imagemapbot.config

import com.mjb2k.imagemapbot.ImageMapBot
import dev.dejvokep.boostedyaml.block.implementation.Section

class ConfigManager(private val plugin: ImageMapBot) : CustomConfig(plugin, "config.yml") {

    class ImageMaps(private val section: Section) {
        val debug get() = section.requireBoolean("debug")
        val channels get() = section.requireStringList("channels")
        val path get() = section.requireTrimmedString("path")
        val reply get() = section.requireBoolean("reply")
    }

    val imagemaps get() = ImageMaps(config.getSection("imagemaps"))
    val discordToken get() = config.getString("discord-token")?.trim()

}
