package com.github.secretx33.zapchest.eventlistener.player

import arrow.core.some
import com.github.secretx33.zapchest.manager.StorageManager
import com.github.secretx33.zapchest.util.extension.isDragOnTopInventory
import com.github.secretx33.zapchest.util.extension.isItemPlace
import com.github.secretx33.zapchest.util.extension.runSync
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin

class PlayerItemMoveToStorageListener(
    private val plugin: Plugin,
    private val storageManager: StorageManager,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryDragEvent.onItemDrag() {
        val holder = view.topInventory.holder as? BlockInventoryHolder ?: return
        val player = whoClicked as? Player ?: return
        // no item was dragged on top inventory
        if(!isDragOnTopInventory()) return
        // perform item move tasks
        runSync(plugin, 50L) { player.parseItemMove(holder) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun InventoryClickEvent.onItemMove() {
        if(didNothing || !isItemPlace) return
        val holder = view.topInventory.holder as? BlockInventoryHolder ?: return
        val player = whoClicked as? Player ?: return
        // perform item move tasks
        runSync(plugin, 50L) { player.parseItemMove(holder) }
    }

    private val InventoryClickEvent.didNothing get() = clickedInventory == null || action == InventoryAction.NOTHING

    private fun Player.parseItemMove(holder: BlockInventoryHolder) = storageManager.parseItemMove(holder, this.some())
}
