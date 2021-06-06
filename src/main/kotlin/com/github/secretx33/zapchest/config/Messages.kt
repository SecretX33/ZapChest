package com.github.secretx33.zapchest.config

import com.github.secretx33.secretcfg.core.config.ConfigOptions
import com.github.secretx33.secretcfg.core.manager.YamlManager
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

class Messages(plugin: Plugin, private val adventureMessage: AdventureMessage) {
    private val manager = YamlManager(plugin, plugin.dataFolder.toPath(), Path("messages"), plugin.logger, ConfigOptions())
    private val stringCache = ConcurrentHashMap<MessageKeys, Component>()
    private val listCache = ConcurrentHashMap<MessageKeys, List<Component>>()

    fun get(key: MessageKeys, default: String? = null): Component
        = stringCache.getOrPut(key) {
            manager.getString(key.configEntry)?.parse() ?: default?.parse() ?: (key.default as? Component) ?: (key.default as String).parse()
        }


    fun getList(key: MessageKeys): List<Component>
        = listCache.getOrPut(key) {
            manager.getStringList(key.configEntry).map { it.parse() }
        }

    suspend fun reload() {
        stringCache.clear()
        listCache.clear()
        manager.reload()
    }

    private fun String.parse(): Component = adventureMessage.parse(this)
}

enum class MessageKeys(val default: Any) {
    CONSOLE_CANNOT_USE("<#FF5555>Sorry, the console cannot use this command."),
    CANNOT_CREATE_GROUP_ALREADY_EXISTS("<#FF5555>You already own a group named <#FFAA00><name><#FF5555>, try with another name or remove that group by using <#FFAA00>/zapchest removegroup <name><#FF5555>."),
    CREATED_GROUP("<#55FF55>Successfully created group <#FFAA00><name><#55FF55>!."),
    CONFIGS_RELOADED("<#55FF55>Reloaded configs."),
    INFERNAL_MOB_DEATH_MESSAGES(""),
    INFERNAL_MOB_SPAWN_MESSAGES(""),
    INFERNAL_MOB_TYPE_DOESNT_EXIST("<#FF5555>Sorry, the infernal type <type> <#FF5555>doesn't exist in your <#FFAA00>mobs.yml <#FF5555>file, please type a valid name."),
    INVALID_NUMBER("<#FF5555>Sorry, <#FFAA00><number> <#FF5555>is not a number."),
    KILLED_ALL_INFERNALS("<#55FF55>Killed all infernals from all worlds."),
    LOOT_ITEM_DOESNT_EXIST("<#FF5555>Sorry, loot item named <#FFAA00><item> <#FF5555>doesn't exist."),
    NOT_TARGETING_INFERNAL("<#FF5555>The entity you're currently targeting is not an Infernal Mob, please target an Infernal Mob and try again."),
    NOT_TARGETING_LIVING_ENTITY("<#FF5555>You are not targeting an entity, please aim to an entity and try again."),
    NOT_TARGETING_VALID_INFERNAL("<#FF5555>The entity you're currently targeting was an Infernal Mob from mob category <group>, but this mob category is not present on your mobs.yml file, so it's not currently considered an Infernal Mob."),
    RECEIVED_LOOT_ITEM("<#55FF55>Received <#FFAA00><amount> <#55FF55>of <#FFAA00><item>."),
    RUST_CORRODE_TOOLS_MESSAGE("<#c27c21>You feel your tools corroding at your hands."),
    TARGETING_INFERNAL("<#55FF55>The <#00AA00><entity> <#55FF55>you're currently targeting has the following abilities: <#ffb319><abilities>."),
    THIEF_MESSAGE_TO_TARGET("<#55FFFF>Woah, beware! <entity> <#55FFFF>stole your <#FFFFFF><item>."),
    THIEF_MESSAGE_TO_TARGET_ITEM_BROKE("<#55FFFF>Woah, beware! <entity> <#55FFFF>stole your <#FFFFFF><item><#55FFFF>, and unfortunately it broke in the process.");

    val configEntry = name.lowercase(Locale.US).replace('_','-')
}

fun String.toComponent(r: Int, g: Int, b: Int) = Component.text(this, TextColor.color(r, g, b))

fun String.toComponent(color: NamedTextColor? = null) = if(color == null) Component.text(this) else Component.text(this, color)

fun Component.replace(oldText: String, newText: String) = replaceText { it.match(oldText).replacement(newText) }

fun Component.replace(oldText: String, newText: String, color: NamedTextColor) = replaceText { it.match(oldText).replacement(newText.toComponent(color)) }

fun Component.replace(oldText: String, newText: ComponentLike) = replaceText { it.match(oldText).replacement(newText) }
