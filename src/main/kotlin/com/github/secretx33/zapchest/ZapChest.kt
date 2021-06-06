package com.github.secretx33.zapchest

import com.cryptomorin.xseries.XMaterial
import com.github.secretx33.zapchest.util.other.CustomKoinComponent
import com.github.secretx33.zapchest.util.other.getOrNull
import com.github.secretx33.zapchest.util.other.loadKoinModules
import com.github.secretx33.zapchest.util.other.startKoin
import com.github.secretx33.zapchest.util.other.stopKoin
import com.github.secretx33.zapchest.util.other.unloadKoinModules
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module

class ZapChest : JavaPlugin(), CustomKoinComponent {

    private val mod = module {
        single<Plugin> { this@ZapChest } bind JavaPlugin::class
        single { get<Plugin>().logger }
        single { AdventureMessage.create() }
        single { BukkitAudiences.create(get()) }
    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        (ItemStack(Material.AIR) as CraftItemStack)
        XMaterial.
    }

    override fun onDisable() {
        getOrNull<BukkitAudiences>()?.close()
        unloadKoinModules(mod)
        stopKoin()
    }
}
