package com.github.secretx33.zapchest.eventlistener

import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class StoragePutItemListener(
    plugin: Plugin,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }


}
