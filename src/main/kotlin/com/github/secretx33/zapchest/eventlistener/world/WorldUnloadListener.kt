package com.github.secretx33.zapchest.eventlistener.world

import com.github.secretx33.zapchest.manager.StorageManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.plugin.Plugin

class WorldUnloadListener(
    plugin: Plugin,
    private val storageManager: StorageManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun WorldUnloadEvent.onWorldUnload(){

    }
}
