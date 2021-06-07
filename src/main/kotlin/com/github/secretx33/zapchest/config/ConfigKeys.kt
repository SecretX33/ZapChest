package com.github.secretx33.zapchest.config

import com.github.secretx33.secretcfg.bukkit.ConfigProvider
import com.github.secretx33.secretcfg.bukkit.enumconfig.EnumConfig
import com.github.secretx33.secretcfg.core.enumconfig.ConfigEnum
import org.bukkit.plugin.Plugin
import java.nio.file.Paths
import java.util.logging.Logger

class Config(plugin: Plugin, logger: Logger) : EnumConfig<ConfigKey> by ConfigProvider.createEnumBased(plugin, Paths.get("config"), ConfigKey::class) {

    init {
        listener { logger.info("[FileWatcher] Detected changes on file $file, reapplying configs.") }
    }
}

enum class ConfigKey(override val path: String, override val default: Any) : ConfigEnum {
    INVITE_EXPIRE("invite-expire", 20.0),
}
