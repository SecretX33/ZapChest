package com.github.secretx33.zapchest.commands.subcommands.storage

import arrow.core.getOrElse
import com.github.secretx33.zapchest.commands.subcommands.PlayerSubCommand
import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.config.replace
import com.github.secretx33.zapchest.config.toComponent
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.extension.formattedTypeName
import com.github.secretx33.zapchest.util.extension.sendMessage
import com.github.secretx33.zapchest.util.mapping.MaterialCategory
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.BlockInventoryHolder

class UnbindMaterialCategoryCommand(
    messages: Messages,
    private val groupRepo: GroupRepo,
) : PlayerSubCommand(messages) {

    override val name: String = "unbindmaterialcategory"
    override val permission: String = "groups.create"
    override val aliases: Set<String> = setOf(name, "unbindcategory", "umc", "uc")

    override fun onCommandByPlayer(player: Player, alias: String, strings: Array<String>) {
        if(strings.size < 3) {
            player.sendMessage("Usage: /$alias $name <group_name> <category> [other_category]".toComponent(
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

        // if block is not a receiver of that group
        if(!groupRepo.hasReceiver(group, holder)) {
            player.sendMessage(messages.get(MessageKeys.BLOCK_IS_NOT_A_RECEIVER_OF_GROUP)
                .replace("<block>", block.formattedTypeName())
                .replace("<group>", group.name))
            return
        }

        var categories = strings.toList().subList(2, strings.size)

        // parse categories and warn user about invalid ones
        categories.filter { !MaterialCategory.hasCategory(it) }.forEach {
            player.sendMessage(messages.get(MessageKeys.MATERIAL_CATEGORY_DOESNT_EXIST).replace("<category>", it))
        }
        categories = categories.filter { MaterialCategory.hasCategory(it) }
        val materials = categories.flatMap { MaterialCategory.getMaterials(it) }

        groupRepo.removeMaterialsOfReceiver(group, holder, materials)
        player.sendMessage(messages.get(MessageKeys.REMOVED_MATERIAL_CATEGORY_OF_RECEIVER)
            .replace("<category>", categories.joinToString())
            .replace("<group>", group.name))
    }

    override fun getCompletor(sender: CommandSender, length: Int, hint: String, strings: Array<String>): List<String> {
        TODO("Not yet implemented")
    }
}