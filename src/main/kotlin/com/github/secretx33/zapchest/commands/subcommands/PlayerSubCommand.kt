package com.github.secretx33.zapchest.commands.subcommands

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.util.extension.sendMessage
import org.bukkit.command.CommandSender

abstract class PlayerSubCommand(protected val messages: Messages) : SubCommand() {

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
    }
}
