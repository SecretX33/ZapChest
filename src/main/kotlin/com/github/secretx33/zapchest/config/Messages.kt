package com.github.secretx33.zapchest.config

import com.github.secretx33.secretcfg.core.config.ConfigOptions
import com.github.secretx33.secretcfg.core.manager.YamlManager
import kotlinx.coroutines.delay
import me.mattstudios.msg.adventure.AdventureMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.plugin.Plugin
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.io.path.Path

class Messages(plugin: Plugin, logger: Logger, private val adventureMessage: AdventureMessage) {
    private val manager = YamlManager(plugin, plugin.dataFolder.toPath(), Path("messages"), logger, ConfigOptions())
    private val stringCache = ConcurrentHashMap<MessageKeys, Component>()
    private val listCache = ConcurrentHashMap<MessageKeys, List<Component>>()

    init {
        manager.listener {
            logger.info("[FileWatcher] Detected changes on file ${manager.fileName}, reapplying configs.")
            delay(150L)
            reload()
        }
    }

    fun get(key: MessageKeys, default: String? = null): Component
        = stringCache.getOrPut(key) {
            manager.getString(key.configEntry)?.parse() ?: default?.parse() ?: (key.default as? Component) ?: (key.default as String).parse()
        }

    fun getList(key: MessageKeys): List<Component>
        = listCache.getOrPut(key) {
            manager.getStringList(key.configEntry).map { it.parse() }
        }

    suspend fun reload() {
        manager.reload()
        stringCache.clear()
        listCache.clear()
    }

    private fun String.parse(): Component = adventureMessage.parse(this)
}

enum class MessageKeys(val default: Any) {
    ADDED_BLOCK_AS_RECEIVER("<#55FF55>Successfully added block <block> as receiver of group <#FFAA00><group>."),
    ADDED_BLOCK_AS_SENDER("<#55FF55>Successfully added block <block> as sender of group <#FFAA00><group>."),
    BLOCK_IS_ALREADY_RECEIVER("<#FF5555>The block you're currently aiming at (<block>) is already added as receiver of group <#FFAA00><group><#FF5555>."),
    BLOCK_IS_ALREADY_SENDER("<#FF5555>The block you're currently aiming at (<block>) is already added as sender of group <#FFAA00><group><#FF5555>."),
    BLOCK_IS_NOT_INVENTORY_HOLDER("<#FF5555>The block you're currently aiming at (<block>) cannot hold items, thus it cannot be added to any ZapChest group."),
    CANNOT_ADD_RECEIVER_AS_SENDER("<#FF5555>The block you're currently aiming at (<block>) is already added as receiver of group <#FFAA00><group><#FF5555>. If you want to add it as sender, please remove it first by using <#FFAA00>/zapchest removereceiver <group><#FF5555>."),
    CANNOT_ADD_SENDER_AS_RECEIVER("<#FF5555>The block you're currently aiming at (<block>) is already added as sender of group <#FFAA00><group><#FF5555>. If you want to add it as receiver, please remove it first by using <#FFAA00>/zapchest removesender <group><#FF5555>."),
    CANNOT_CREATE_GROUP_ALREADY_EXISTS("<#FF5555>You already own a group named <#FFAA00><group><#FF5555>, try with another name or remove that group by using <#FFAA00>/zapchest removegroup <group><#FF5555>."),
    CONFIGS_RELOADED("<#55FF55>Reloaded configs."),
    CONSOLE_CANNOT_USE("<#FF5555>Sorry, the console cannot use this command."),
    CREATED_GROUP("<#55FF55>Successfully created group <#FFAA00><group><#55FF55>."),
    GROUP_NOT_FOUND("<#FF5555>Group named <#FFAA00><group><#FF5555> could not be found."),
    GROUP_WAS_DELETED("<#FF5555>Couldn't join ZapChest group <#FFAA00><group><#FF5555> because it was deleted."),
    INVITE_NOT_FOUND("<#FF5555>Invite for ZapChest group <#FFAA00><group><#FF5555> could not be found."),
    INVITED_PLAYERS_TO_GROUP("<#55FF55>Invited player(s) <#FFAA00><players><#55FF55> to group <#FFAA00><group><#55FF55>!"),
    PLAYER_ALREADY_IN_THAT_GROUP("<#FF5555>Player <#FFAA00><player><#FF5555> already belongs to group <#FFAA00><group><#FF5555>."),
    PLAYER_GROUP_LIST_MEMBER_OF_GROUP_SUFFIX("(member)"),
    PLAYER_GROUP_LIST_OWNER_GROUP_SUFFIX("(owner)"),
    PLAYER_GROUPS_LIST("[ZapChest] <#55FF55>You currently are is these group(s): <#FFAA00><groups><#55FF55>."),
    PLAYER_IS_NOT_IN_ANY_GROUP("<#55FF55>You currently don't own or belong to any group."),
    PLAYER_JOINED_GROUP("<#55FF55>Player <#FFAA00><player><#55FF55> just joined ZapChest group <#FFAA00><group><#55FF55>!"),
    PLAYER_NOT_FOUND("<#FF5555>Player named <#FFAA00><player><#FF5555> could not be found."),
    RECEIVED_INVITE_TO_GROUP("<#2afab5><owner> invited you to join their ZapChest group <#FFAA00><group><#2afab5>\n[<#55FF55>**\\[accept\\]**](command: /zapchest acceptinvite <group>)"),
    STORAGE_WAS_DESTROYED("<#FF5555>You just broke a Storage, as result it was unbounded from all groups it belonged."),
    SUCCESSFULLY_JOINED_GROUP("<#55FF55>You just joined ZapChest group <#FFAA00><group><#55FF55>.");

    val configEntry = name.lowercase(Locale.US).replace('_','-')
}

fun String.toComponent(r: Int, g: Int, b: Int) = Component.text(this, TextColor.color(r, g, b))

fun String.toComponent(color: NamedTextColor? = null) = if(color == null) Component.text(this) else Component.text(this, color)

fun Component.replace(oldText: String, newText: String) = replaceText { it.match(oldText).replacement(newText) }

fun Component.replace(oldText: String, newText: String, color: NamedTextColor) = replaceText { it.match(oldText).replacement(newText.toComponent(color)) }

fun Component.replace(oldText: String, newText: ComponentLike) = replaceText { it.match(oldText).replacement(newText) }
