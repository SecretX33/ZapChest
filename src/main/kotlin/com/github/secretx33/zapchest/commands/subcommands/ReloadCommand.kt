package com.github.secretx33.zapchest.commands.subcommands

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.util.extension.sendMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand (
    private val messages: Messages,
): SubCommand() {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: Set<String> = setOf(name, "rel", "r")

    private val reloadLock = Semaphore(1)

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        onCommandByConsole(player, alias, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        CoroutineScope(Dispatchers.Default).launch {
            reloadLock.withPermit {
                messages.reload()
                sender.sendMessage(messages.get(MessageKeys.CONFIGS_RELOADED))
            }
        }
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> = emptyList()
}
