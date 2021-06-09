package com.github.secretx33.zapchest.commands.subcommands.group

import com.github.secretx33.zapchest.commands.subcommands.PlayerSubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.sendMessage
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ListGroupsCommand(
    messages: Messages,
    private val groupRepo: GroupRepo,
) : PlayerSubCommand(messages) {

    override val name: String = "listgroups"
    override val permission: String = "groups.listgroups"
    override val aliases: Set<String> = setOf(name, "list", "lg")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        val groups = groupRepo.getGroupsThatPlayerBelongs(player)

        // player is not in any group
        if(groups.isEmpty()) {
            player.sendMessage(messages.get(MessageKeys.PLAYER_IS_NOT_IN_ANY_GROUP))
            return
        }

        val message = messages.get(MessageKeys.PLAYER_GROUPS_LIST).replace("<groups>", groups.formatGroupList(player))
        player.sendMessage(message)
    }

    // format a list of groups to be displayed for the specified player
    private fun List<Group>.formatGroupList(player: Player): Component {
        val cb = Component.text()

        forEachIndexed { index, group ->
            cb.append { group.name.toComponent() }
            cb.append { " ".toComponent() }

            if(group.owner.uniqueId == player.uniqueId)
                cb.append(ownerSuffix)
            else
                cb.append(memberSuffix)

            if(index < lastIndex) cb.append(", ".toComponent())
        }
        return cb.build()
    }

    private val ownerSuffix: Component get() = messages.get(MessageKeys.PLAYER_GROUP_LIST_OWNER_GROUP_SUFFIX)

    private val memberSuffix: Component get() = messages.get(MessageKeys.PLAYER_GROUP_LIST_MEMBER_OF_GROUP_SUFFIX)

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> = emptyList()
}
