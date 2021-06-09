package com.github.secretx33.zapchest.commands.subcommands.storage.addremove

import com.github.secretx33.zapchest.commands.subcommands.PlayerSubCommand
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.repository.GroupRepo
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class AbstractStorageAddRemoveCommand(
    messages: Messages,
    protected val groupRepo: GroupRepo,
) : PlayerSubCommand(messages) {

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length != 2) return emptyList()

        return groupRepo.getGroupsThatPlayerOwns(sender)
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }
    }
}
