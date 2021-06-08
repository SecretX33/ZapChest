package com.github.secretx33.zapchest.commands.subcommands.storage

import arrow.core.getOrElse
import com.github.secretx33.zapchest.commands.subcommands.SubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.formattedTypeName
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder

class AddReceiverCommand(
    private val messages: Messages,
    private val groupRepo: GroupRepo,
) : SubCommand() {

    override val name: String = "addreceiver"
    override val permission: String = "groups.addstorage"
    override val aliases: List<String> = listOf(name, "addr", "ar")

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

        // if block is already a sender of that group
        if(groupRepo.hasSender(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.CANNOT_ADD_SENDER_AS_RECEIVER)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        // if block is already a receiver of that group
        if(groupRepo.hasReceiver(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_ALREADY_RECEIVER)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        // add block as receiver to that group
        groupRepo.addReceiver(group, block)
        player.sendMessage(messages.get(MessageKeys.ADDED_BLOCK_AS_RECEIVER)
            .replace("<block>", block.formattedTypeName())
            .replace("<group>", group.name))
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length != 2) return emptyList()

        return groupRepo.getGroupsThatPlayerOwns(sender)
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
    }
}
