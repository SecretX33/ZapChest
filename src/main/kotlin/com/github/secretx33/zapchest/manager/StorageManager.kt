package com.github.secretx33.zapchest.manager

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.toLocalPlayer
import com.github.secretx33.zapchest.repository.GroupRepo
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.inventory.Inventory

class StorageManager(
    private val groupRepo: GroupRepo,
) {

    fun parseItemMove(holder: BlockInventoryHolder, player: Option<Player> = None) {
        val inventory = holder.inventory
        val location = inventory.location ?: return
        val groups = groupRepo.getSenderGroupsAt(location).takeIf { it.isNotEmpty() }?.filterGroups(player)
            ?: return

        groups.forEach { moveItems(inventory, it) }
    }

    private fun moveItems(inventory: Inventory, group: Group) {
        inventory.contents.filter { it?.type?.isAir == false }.forEach outerLoop@{ item ->
            val receivers = group.receiversFor(item.type)

            receivers.forEach {
                val leftover = it.contents.addItem(item)
                // if(leftover[0]?.amount != item.amount) TODO("Do some sorting on the chest")
                // all items fit in the inventory
                if (leftover.isEmpty()) {
                    item.amount = 0
                } else {
                    item.amount -= item.amount - (leftover[0]?.amount ?: 0)
                }
                if (item.amount <= 0) return@outerLoop
            }
        }
    }

    private fun Collection<Group>.filterGroups(player: Option<Player>): Collection<Group> = when(player) {
        is Some -> {
            val localPlayer = player.value.toLocalPlayer()
            filter { it.owner == localPlayer } + filter { localPlayer in it.members }
        }
        is None -> this
    }
}
