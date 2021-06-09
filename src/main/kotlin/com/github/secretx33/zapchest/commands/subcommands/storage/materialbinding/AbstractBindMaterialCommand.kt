package com.github.secretx33.zapchest.commands.subcommands.storage.materialbinding

import arrow.core.getOrElse
import com.github.secretx33.zapchest.commands.subcommands.PlayerSubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.formattedTypeName
import com.github.secretx33.zapchest.util.extension.runSync
import com.github.secretx33.zapchest.util.extension.sendMessage
import com.github.secretx33.zapchest.util.mapping.MaterialCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin
import java.util.Locale

abstract class AbstractBindMaterialCommand(
    private val plugin: Plugin,
    messages: Messages,
    protected val groupRepo: GroupRepo,
) : PlayerSubCommand(messages) {

    protected abstract val operation: (Group, BlockInventoryHolder, Collection<Material>) -> Unit

    protected abstract val successMessage: MessageKeys

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 3) {
            player.sendMessage("Usage: /$alias $name <group_name> <material> [other_material]".toComponent(
                NamedTextColor.RED))
            return
        }
        // parse group
        val group = groupRepo.getGroup(player, strings[1]).getOrElse {
            player.sendMessage(messages.get(MessageKeys.GROUP_NOT_FOUND).replace("<group>", strings[1]))
            return
        }

        // get targeted block
        val block = player.getTargetBlock(null, 10)
        val holder = block.state as? BlockInventoryHolder ?: run {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_NOT_INVENTORY_HOLDER).replace("<block>", block.formattedTypeName()))
            return
        }

        // switch to coroutine because of regex matching, it can be kinda cpu consuming
        CoroutineScope(Dispatchers.Default).launch {

            // if block is not a receiver of that group
            if(!groupRepo.hasReceiver(group, holder)) {
                player.sendMessage(messages.get(MessageKeys.BLOCK_IS_NOT_A_RECEIVER_OF_GROUP)
                    .replace("<block>", block.formattedTypeName())
                    .replace("<group>", group.name))
                return@launch
            }

            // parse materials and warn user about invalid ones
            val materials = strings.toList().subList(2, strings.size)
                .map { it.prepareMaterialRegex() }
                .flatMap { regex -> MaterialCategory.nonLegacyItemMaterials.filter { regex.matches(it.name) } }
                .takeUnless { it.isEmpty() } ?: return@launch

            runSync(plugin) { operation(group, holder, materials) }
            player.sendMessage(messages.get(successMessage)
                .replace("<materials>", materials.sortedBy { it.name }.joinToString { it.name.lowercase(Locale.US) })
                .replace("<group>", group.name))
        }
    }

    private fun String.prepareMaterialRegex(): Regex = "(?i)${replace("*", ".*")}".toRegex()

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        if(sender !is Player || length < 2) return emptyList()

        if(length == 2) return groupRepo.getGroupsThatPlayerOwns(sender)
            .filter { it.name.startsWith(hint, ignoreCase = true) }
            .map { it.name }

        val typedMaterials = strings.toList().subList(2, strings.lastIndex)

        return MaterialCategory.nonLegacyItemMaterials.filter { it.name.startsWith(hint, ignoreCase = true) && typedMaterials.none { typed -> typed.equals(it.name, ignoreCase = true) } }
            .map { it.name.lowercase(Locale.US) }
    }
}
