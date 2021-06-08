package com.github.secretx33.zapchest.commands.subcommands.invite

import arrow.core.getOrElse
import com.github.secretx33.zapchest.commands.subcommands.SubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.manager.GroupInviteManager
import com.github.secretx33.zapchest.model.toLocalPlayer
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class InviteMembersCommand(
    private val messages: Messages,
    private val groupRepo: GroupRepo,
    private val groupInviteManager: GroupInviteManager,
) : SubCommand() {

    override val name: String = "invitemembers"
    override val permission: String = "groups.addothers"
    override val aliases: List<String> = listOf(name, "addm", "am")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 3) {
            player.sendMessage("Usage: /$alias $name <group_name> <player_name> [player_name2...]".toComponent(NamedTextColor.RED))
            return
        }

        // group with specified name was not be found
        val group = groupRepo.getGroup(player, strings[1]).getOrElse { player.sendMessage(messages.get(MessageKeys.GROUP_NOT_FOUND).replace("<group>", strings[1]))
            return
        }

        // gets all players specified (up until 8 at a time)
        var newMembers = strings.toList().subList(2, strings.size.coerceAtMost(10))
            .mapNotNull { Bukkit.getPlayerExact(it) ?: run {
                player.sendMessage(messages.get(MessageKeys.PLAYER_NOT_FOUND).replace("<player>", strings[2]))
                null }
            }.filter { it.uniqueId != player.uniqueId }  // player cannot invite themselves

        // player is already member of that group
        newMembers.filter { it.toLocalPlayer() in group.members }.forEach { newMember ->
            player.sendMessage(messages.get(MessageKeys.PLAYER_ALREADY_IN_THAT_GROUP).replace("<player>", newMember.name).replace("<group>", group.name))
            return
        }

        // filter to only members that are not already in the group
        newMembers = newMembers.filter { it.toLocalPlayer() !in group.members }

        // inform group owner about invites
        val newMembersNames = newMembers.takeIf { it.isNotEmpty() }?.sortedBy { it.name }?.joinToString { it.name } ?: return
        player.sendMessage(messages.get(MessageKeys.INVITED_PLAYERS_TO_GROUP).replace("<players>", newMembersNames).replace("<group>", group.name))

        // invites newMembers to the group
        newMembers.forEach {
            groupInviteManager.createInvite(it, group)
            it.sendMessage(messages.get(MessageKeys.RECEIVED_INVITE_TO_GROUP).replace("<owner>", player.name).replace("<group>", group.name))
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) {
            if(hint.isNotBlank()) return emptyList()
            val groups = groupRepo.getGroupsThatPlayerOwns(sender)

            return groups.takeIf { it.isNotEmpty() }?.filter { it.name.startsWith(hint, ignoreCase = true) }?.map { it.name } ?: listOf("<none>")
        }

        return Bukkit.getOnlinePlayers().asSequence()
            .filter { it.uniqueId != sender.uniqueId && it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
            .toList()
    }
}
