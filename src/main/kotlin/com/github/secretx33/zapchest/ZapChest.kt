package com.github.secretx33.zapchest

import com.github.secretx33.zapchest.commands.Commands
import com.github.secretx33.zapchest.config.Config
import com.github.secretx33.zapchest.config.Messages
import com.github.secretx33.zapchest.database.SQLite
import com.github.secretx33.zapchest.eventlistener.block.BlockItemMoveToStorageListener
import com.github.secretx33.zapchest.eventlistener.player.PlayerItemMoveToStorageListener
import com.github.secretx33.zapchest.eventlistener.player.StorageBreakListener
import com.github.secretx33.zapchest.manager.GroupInviteManager
import com.github.secretx33.zapchest.manager.StorageManager
import com.github.secretx33.zapchest.repository.GroupRepo
import com.github.secretx33.zapchest.util.other.CustomKoinComponent
import com.github.secretx33.zapchest.util.other.get
import com.github.secretx33.zapchest.util.other.getOrNull
import com.github.secretx33.zapchest.util.other.loadKoinModules
import com.github.secretx33.zapchest.util.other.startKoin
import com.github.secretx33.zapchest.util.other.stopKoin
import com.github.secretx33.zapchest.util.other.unloadKoinModules
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.platform.bukkit.BukkitAudiences
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
        single { Config(get(), get()) }
        single { Messages(get(), get(), get()) }
        single { SQLite(get(), get(), get()) }
        single { GroupRepo(get()) }
        single { StorageManager(get()) }
        single { GroupInviteManager(get()) }
        single { BlockItemMoveToStorageListener(get(), get()) }
        single { PlayerItemMoveToStorageListener(get(), get()) }
        single { StorageBreakListener(get(), get(), get()) }
        single { Commands(get()) }
    }

    override fun onEnable() {
        startKoin {
            printLogger(Level.ERROR)
            loadKoinModules(mod)
        }
        get<BlockItemMoveToStorageListener>()
        get<PlayerItemMoveToStorageListener>()
        get<StorageBreakListener>()
        get<Commands>()
    }

    override fun onDisable() {
        getOrNull<BukkitAudiences>()?.close()
        unloadKoinModules(mod)
        stopKoin()
    }
}
