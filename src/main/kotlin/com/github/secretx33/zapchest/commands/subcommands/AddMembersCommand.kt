package com.github.secretx33.zapchest.commands.subcommands

import arrow.core.getOrElse
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AddMembersCommand(
    private val messages: Messages,
    private val groupRepo: GroupRepo,
) : SubCommand() {

    override val name: String = "addmember"
    override val permission: String = "group.addothers"
    override val aliases: List<String> = listOf(name, "addm")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 3) {
            player.sendMessage("Usage: /$alias $name <group_name> <player_name> [player_name2...]".toComponent(NamedTextColor.RED))
            return
        }

        // group with specified name was not be found
        val group = groupRepo.getGroup(player, strings[1]).getOrElse { player.sendMessage(messages.get(MessageKeys.GROUP_NOT_FOUND).replace("<group>", strings[1]))
            return
        }

        // player with specified name was not found
        val newMember = Bukkit.getPlayerExact(strings[2]) ?: run { player.sendMessage(messages.get(MessageKeys.PLAYER_NOT_FOUND).replace("<player>", strings[2]))
            return
        }

        // player is already member of that group
        if(newMember.uniqueId in group.members) {
            player.sendMessage(messages.get(MessageKeys.PLAYER_ALREADY_IN_THAT_GROUP).replace("<player>", newMember.name).replace("<group>", group.name))
            return
        }

        // invites newMember to the group
        player.sendMessage(messages.get(MessageKeys.INVITED_PLAYER_TO_GROUP).replace("<player>", newMember.name).replace("<group>", group.name))

        newMember.sendMessage(messages.get(MessageKeys.RECEIVED_INVITE_TO_GROUP).replace("<owner>", newMember.name).replace("<group>", group.name))
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player) return emptyList()

        if(length == 1) return groupRepo.getGroups(sender).filter { it.name.startsWith(hint, ignoreCase = true) }.map { it.name }

        return Bukkit.getOnlinePlayers().asSequence()
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
            .toList()
    }
}
