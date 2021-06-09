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

class AddSenderCommand(
    messages: Messages,
    groupRepo: GroupRepo,
) : AbstractStorageAddRemoveCommand(messages, groupRepo) {

    override val name: String = "addsender"
    override val permission: String = "groups.addstorage"
    override val aliases: Set<String> = setOf(name, "adds", "as")

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

        // if block is already a receiver of that group
        if(groupRepo.hasReceiver(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.CANNOT_ADD_RECEIVER_AS_SENDER)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        // if block is already a sender of that group
        if(groupRepo.hasSender(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_ALREADY_SENDER)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        // add block as sender to that group
        groupRepo.addSender(group, block)
        player.sendMessage(messages.get(MessageKeys.ADDED_BLOCK_AS_SENDER)
            .replace("<block>", block.formattedTypeName())
            .replace("<group>", group.name))
    }
}
