package com.github.secretx33.zapchest.commands.subcommands.invite

import com.github.secretx33.zapchest.commands.subcommands.SubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.manager.GroupInviteManager
import com.github.secretx33.zapchest.model.GroupJoinResponse
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

class AcceptInviteCommand (
    private val messages: Messages,
    private val groupInviteManager: GroupInviteManager,
) : SubCommand() {

    override val name: String = "acceptinvite"
    override val permission: String = "groups.joinothers"
    override val aliases: List<String> = listOf(name, "accept", "ai")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 2) {
            player.sendMessage("Usage: /$alias $name <group_name>".toComponent(NamedTextColor.RED))
            return
        }

        val groupName = strings[1].lowercase(Locale.US)
        val response = groupInviteManager.acceptInvite(player, groupName)

        when(response.reason) {
            GroupJoinResponse.Reason.GROUP_REMOVED -> player.sendMessage(messages.get(MessageKeys.GROUP_WAS_DELETED).replace("<group>", groupName))
            GroupJoinResponse.Reason.NON_EXISTENT -> player.sendMessage(messages.get(MessageKeys.INVITE_NOT_FOUND).replace("<group>", response.group?.name ?: groupName))
            GroupJoinResponse.Reason.JOINED -> alertAboutNewMember(player, groupName, response)
        }
    }

    private fun alertAboutNewMember(
        player: Player,
        groupName: String,
        response: GroupJoinResponse
    ) {
        player.sendMessage(messages.get(MessageKeys.SUCCESSFULLY_JOINED_GROUP).replace("<group>", groupName))
        val group = response.group ?: return
        val message = messages.get(MessageKeys.PLAYER_JOINED_GROUP).replace("<player>", player.name)
            .replace("<group>", group.name)

        (group.members + group.owner).asSequence().filter { it.uniqueId != player.uniqueId }
            .mapNotNull { Bukkit.getPlayer(it.uniqueId) }
            .forEach { it.sendMessage(message) }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length != 2) return emptyList()
        return groupInviteManager.getAllInvites(sender).filter { it.startsWith(hint, ignoreCase = true) }
    }


}
