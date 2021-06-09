package com.github.secretx33.zapchest.commands.subcommands.storage.materialbinding

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.repository.GroupRepo
import org.bukkit.Material
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin

class BindMaterialsCommand(
    plugin: Plugin,
    messages: Messages,
    groupRepo: GroupRepo,
) : AbstractBindMaterialCommand(plugin, messages, groupRepo) {

    override val name: String = "bindmaterials"
    override val permission: String = "groups.create"
    override val aliases: Set<String> = setOf(name, "bindm", "bind", "bm")

    override val operation: (Group, BlockInventoryHolder, Collection<Material>) -> Unit = { group, holder, materials -> groupRepo.addMaterialsToReceiver(group, holder, materials) }

    override val successMessage: MessageKeys = MessageKeys.ADDED_MATERIALS_TO_RECEIVER
}
