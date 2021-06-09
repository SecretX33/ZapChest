package com.github.secretx33.zapchest.commands.subcommands.storage.addremove

import arrow.core.getOrElse
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.formattedTypeName
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder

class RemoveSenderCommand(
    messages: Messages,
    groupRepo: GroupRepo,
): AbstractStorageAddRemoveCommand(messages, groupRepo) {

    override val name: String = "removesender"
    override val permission: String = "groups.create"
    override val aliases: Set<String> = setOf(name, "removes", "rs")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <group_name>".toComponent(NamedTextColor.RED))
            return
        }

        val group = groupRepo.getGroup(player, strings[1]).getOrElse {
            player.sendMessage(messages.get(MessageKeys.GROUP_NOT_FOUND).replace("<group>", strings[1]))
            return
        }

        val block = player.getTargetBlock(null, 10)
        val holder = block.state as? BlockInventoryHolder ?: run {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_NOT_INVENTORY_HOLDER).replace("<block>", block.formattedTypeName()))
            return
        }

        // if block is not a sender of that group
        if(!groupRepo.hasSender(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_NOT_A_SENDER_OF_GROUP)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        // removed sender of that group
        groupRepo.removeSender(group, holder)
        player.sendMessage(messages.get(MessageKeys.REMOVED_SENDER_OF_GROUP)
            .replace("<group>", group.name))
    }
}
