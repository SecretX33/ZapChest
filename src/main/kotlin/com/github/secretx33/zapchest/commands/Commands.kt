package com.github.secretx33.zapchest.commands

import com.github.secretx33.zapchest.commands.subcommands.ReloadCommand
import com.github.secretx33.zapchest.commands.subcommands.SubCommand
import com.github.secretx33.zapchest.commands.subcommands.group.CreateGroupCommand
import com.github.secretx33.zapchest.commands.subcommands.group.ListGroupsCommand
import com.github.secretx33.zapchest.commands.subcommands.invite.AcceptInviteCommand
import com.github.secretx33.zapchest.commands.subcommands.invite.InviteMembersCommand
import com.github.secretx33.zapchest.commands.subcommands.storage.AddReceiverCommand
import com.github.secretx33.zapchest.commands.subcommands.storage.AddSenderCommand
import com.github.secretx33.zapchest.commands.subcommands.storage.BindMaterialCategoryCommand
import com.github.secretx33.zapchest.commands.subcommands.storage.UnbindMaterialCategoryCommand
import com.github.secretx33.zapchest.util.other.CustomKoinComponent
import com.github.secretx33.zapchest.util.other.get
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale

class Commands(plugin: JavaPlugin) : CommandExecutor, TabCompleter, CustomKoinComponent {

    private val subcommands: Set<SubCommand> = setOf(
        // group
        CreateGroupCommand(get(), get()),
        ListGroupsCommand(get(), get()),
        // invite
        AcceptInviteCommand(get(), get()),
        InviteMembersCommand(get(), get(), get()),
        // storage
        AddReceiverCommand(get(), get()),
        AddSenderCommand(get(), get()),
        BindMaterialCategoryCommand(get(), get()),
        UnbindMaterialCategoryCommand(get(), get()),
        // general
        ReloadCommand(get()),
    )

    init {
        plugin.getCommand("zapchest")?.let { cmd ->
            cmd.setExecutor(this)
            cmd.tabCompleter = this
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, alias: String, strings: Array<String>): Boolean {
        if (strings.isEmpty()) return true

        val sub = strings[0].lowercase(Locale.US)
        subcommands.firstOrNull { it.hasPermission(sender) && (sub == it.name || sub in it.aliases) }?.let { cmd ->
            if(sender is Player) {
                cmd.onCommandByPlayer(sender, alias, strings)
            } else {
                cmd.onCommandByConsole(sender, alias, strings)
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, strings: Array<String>): List<String> {
        if(strings.isEmpty()) return emptyList()
        // mobs <subcommand> <args>
        if(strings.size == 1) {
            return subcommands.asSequence()
                .filter { cmd -> cmd.hasPermission(sender) && cmd.name.startsWith(strings[0], ignoreCase = true)}
                .map { it.name }
                .toList()
        }
        return subcommands
            .firstOrNull { it.hasPermission(sender) && it.aliases.contains(strings[0].lowercase()) }
            ?.getCompletor(sender, strings.size, strings[strings.size - 1], strings)
            ?: emptyList()
    }
}
