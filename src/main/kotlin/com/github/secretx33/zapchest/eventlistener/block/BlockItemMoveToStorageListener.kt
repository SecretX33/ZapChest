package com.github.secretx33.zapchest.eventlistener.block

import arrow.core.None
import com.github.secretx33.zapchest.manager.StorageManager
import com.github.secretx33.zapchest.util.extension.runSync
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin

class BlockItemMoveToStorageListener (
    private val plugin: Plugin,
    private val storageManager: StorageManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryMoveItemEvent.onItemMove() {
        val holder = destination.holder as? BlockInventoryHolder ?: return

        runSync(plugin, 50L) { storageManager.parseItemMove(holder) }
    }
}
