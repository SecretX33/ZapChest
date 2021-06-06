package com.github.secretx33.zapchest.manager

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.repository.StorageRepo
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class StorageManager(
    private val storageRepo: StorageRepo,
) {

    fun parseItemMove(holder: BlockInventoryHolder, player: Option<Player> = None) {
        val inventory = holder.inventory
        val location = inventory.location ?: return
        val group = storageRepo.getSenderStorages()[location].takeIf { it.isNotEmpty() }?.bestGroup(player) ?: return

        moveItems(inventory, group)
    }

    private fun moveItems(inventory: Inventory, group: Group) {
        inventory.contents.forEachIndexed { index, item: ItemStack? ->
            if (item == null || item.type.isAir) return@forEachIndexed
            val receivers = group.receiversFor(item.type)

            receivers.forEach {
                val leftover = it.contents.addItem(item)
                // if(leftover[0]?.amount != item.amount) TODO("Do some sorting on the chest")
                // all items fit in the inventory
                if (leftover.isEmpty()) {
                    item.amount = 0
                    item.type = Material.AIR
                    return@forEachIndexed
                }
                item.amount -= item.amount - (leftover[0]?.amount ?: 0)
                if (item.amount <= 0) return@forEachIndexed
            }
        }
    }

    private fun Collection<Group>.bestGroup(player: Option<Player>): Group = when(player) {
        is Some -> firstOrNull { it.owner == player.value.uniqueId } ?: first()
        is None -> first()
    }
}
