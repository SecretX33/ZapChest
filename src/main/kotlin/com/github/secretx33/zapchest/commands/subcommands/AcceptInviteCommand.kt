package com.github.secretx33.zapchest.commands.subcommands

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.manager.GroupInviteManager
import com.github.secretx33.zapchest.model.GroupJoinResponse
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.format.NamedTextColor
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

        when(response) {
            GroupJoinResponse.GROUP_REMOVED -> player.sendMessage(messages.get(MessageKeys.GROUP_WAS_DELETED).replace("<group>", groupName))
            GroupJoinResponse.NON_EXISTENT -> player.sendMessage(messages.get(MessageKeys.INVITE_NOT_FOUND).replace("<group>", groupName))
            GroupJoinResponse.JOINED -> player.sendMessage(messages.get(MessageKeys.SUCCESSFULLY_JOINED_GROUP).replace("<group>", groupName))
        }
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player) return emptyList()
        TODO("Not yet implemented")
    }


}
