package com.github.secretx33.zapchest.eventlistener.player

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin

class StorageBreakListener(
    plugin: Plugin,
    private val storageRepo: GroupRepo,
    private val messages: Messages,
) : Listener {

    init { Bukkit.getPluginManager().registerEvents(this, plugin) }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun BlockBreakEvent.onStorageBreak() {
        val holder = block.state as? BlockInventoryHolder ?: return
        if(!storageRepo.isStorage(holder)) return
        player.sendMessage(messages.get(MessageKeys.STORAGE_WAS_DESTROYED))
        storageRepo.unboundStorage(holder)
    }
}
