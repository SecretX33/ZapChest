//package com.github.secretx33.zapchest.commands.subcommands.storage
//
//import com.github.secretx33.zapchest.commands.subcommands.SubCommand
//import com.github.secretx33.zapchest.config.MessageKeys
//import com.github.secretx33.zapchest.config.Messages
//import com.github.secretx33.zapchest.manager.GroupInviteManager
//import com.github.secretx33.zapchest.repository.GroupRepo
//import com.github.secretx33.zapchest.util.extension.sendMessage
//import org.bukkit.command.CommandSender
//
//class RemoveStorageCommand(
//    private val messages: Messages,
//    private val groupRepo: GroupRepo,
//    private val groupInviteManager: GroupInviteManager,
//) : SubCommand() {
//
//    override val name: String = "addstorage"
//    override val permission: String = "groups.addstorage"
//    override val aliases: List<String> = listOf(name, "adds", "as")
//
//    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
//        sender.sendMessage(messages.get(MessageKeys.CONSOLE_CANNOT_USE))
//    }
//}
