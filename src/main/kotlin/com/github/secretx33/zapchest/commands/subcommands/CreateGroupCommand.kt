package com.github.secretx33.zapchest.commands.subcommands

import arrow.core.None
import arrow.core.Some
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.StorageRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateGroupCommand(
    private val messages: Messages,
    private val storageRepo: StorageRepo,
) : SubCommand() {

    override val name: String = "creategroup"
    override val permission: String = "creategroup"
    override val aliases: List<String> = listOf(name, "create", "cg")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <group_name>".toComponent(NamedTextColor.RED))
            return
        }
        val groupName = strings[1]

        val group = storageRepo.getGroup(player, groupName)
        when(group) {
            is None -> {
                storageRepo.addGroup(player, groupName)
                player.sendMessage(messages.get(MessageKeys.CREATED_GROUP).replace("<name>", groupName))
            }
            is Some -> player.sendMessage(messages.get(MessageKeys.CANNOT_CREATE_GROUP_ALREADY_EXISTS).replace("<name>", groupName))
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> = when {
        length == 1 && hint.isBlank() -> listOf("<group_name>")
        else -> emptyList()
    }
}
