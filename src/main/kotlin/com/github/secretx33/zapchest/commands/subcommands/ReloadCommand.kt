package com.github.secretx33.zapchest.commands.subcommands

import com.github.secretx33.zapchest.util.other.CustomKoinComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReloadCommand : SubCommand(), CustomKoinComponent {

    override val name: String = "reload"
    override val permission: String = "reload"
    override val aliases: List<String> = listOf(name, "rel", "r")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        onCommandByConsole(player, alias, strings)
    }

    override fun onCommandByConsole(sender: CommandSender, alias: String, strings: Array<String>) {
        infernalMobsManager.unloadAllInfernals()
        bossBarManager.hideAllBarsFromAllPlayers()
        config.reload()
        messages.reload()
        abilityConfig.reload()
        lootItemsRepo.reload()
        globalDropsRepo.reload()
        infernalMobTypesRepo.reload()
        charmsRepo.reload()
        infernalMobsManager.loadAllInfernals()
        bossBarManager.showBarsOfNearbyInfernalsForAllPlayers()
        charmsManager.reload()
        sender.sendMessage(messages.get(MessageKeys.CONFIGS_RELOADED))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        return emptyList()
    }
}
