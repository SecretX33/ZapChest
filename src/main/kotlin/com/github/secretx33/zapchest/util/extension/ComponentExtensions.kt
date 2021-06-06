package com.github.secretx33.zapchest.util.extension

import com.github.secretx33.zapchest.util.other.CustomKoinComponent
import com.github.secretx33.zapchest.util.extension.AdventureSupplier.audience
import com.github.secretx33.zapchest.util.other.get
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.ComponentLike
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

private object AdventureSupplier : CustomKoinComponent {

    val audience = get<BukkitAudiences>()
}

fun CommandSender.sendMessage(message: ComponentLike) = audience.sender(this).sendMessage(message)
