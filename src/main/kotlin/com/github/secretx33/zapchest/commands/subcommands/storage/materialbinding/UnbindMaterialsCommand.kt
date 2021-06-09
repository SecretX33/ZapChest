package com.github.secretx33.zapchest.commands.subcommands.storage.materialbinding

import com.github.secretx33.zapchest.config.MessageKeys
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.repository.GroupRepo
import org.bukkit.Material
import org.bukkit.inventory.BlockInventoryHolder
import org.bukkit.plugin.Plugin

class UnbindMaterialsCommand(
    plugin: Plugin,
    messages: Messages,
    groupRepo: GroupRepo,
) : AbstractBindMaterialCommand(plugin, messages, groupRepo) {

    override val name: String = "unbindmaterials"
    override val permission: String = "groups.create"
    override val aliases: Set<String> = setOf(name, "unbindm", "unbind", "um")

    override val operation: (Group, BlockInventoryHolder, Collection<Material>) -> Unit = { group, holder, materials -> groupRepo.removeMaterialsOfReceiver(group, holder, materials) }

    override val successMessage: MessageKeys = MessageKeys.REMOVED_MATERIALS_OF_RECEIVER
}
