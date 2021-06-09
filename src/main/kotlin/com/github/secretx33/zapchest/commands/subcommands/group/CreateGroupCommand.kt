package com.github.secretx33.zapchest.commands.subcommands.group

import arrow.core.None
import arrow.core.Some
import com.github.secretx33.zapchest.commands.subcommands.SubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

class CreateGroupCommand(
    private val messages: Messages,
    private val groupRepo: GroupRepo,
) : SubCommand() {

    override val name: String = "creategroup"
    override val permission: String = "groups.create"
    override val aliases: Set<String> = setOf(name, "create", "cg")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <group_name>".toComponent(NamedTextColor.RED))
            return
        }
        val groupName = strings[1].substring(0, strings[1].length.coerceAtMost(40)).lowercase(Locale.US)
        val group = groupRepo.getGroup(player, groupName)

        when(group) {
            is None -> {
                groupRepo.createGroup(player, groupName)
                player.sendMessage(messages.get(MessageKeys.CREATED_GROUP).replace("<group>", groupName))
            }
            is Some -> player.sendMessage(messages.get(MessageKeys.CANNOT_CREATE_GROUP_ALREADY_EXISTS)
                .replace("<group>", groupName))
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> = when {
        sender !is Player -> emptyList()
        length == 2 && hint.isBlank() -> listOf("<group_name>")
        else -> emptyList()
    }
}
