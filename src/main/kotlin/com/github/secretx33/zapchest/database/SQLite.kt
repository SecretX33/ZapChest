package com.github.secretx33.zapchest.database

import com.github.secretx33.zapchest.config.Config
import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.LocalPlayer
import com.github.secretx33.zapchest.model.Storage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.lang.reflect.Type
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID
import java.util.logging.Logger
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

class SQLite(plugin: Plugin, private val logger: Logger, private val config: Config) {

    private val dbFile = plugin.dataFolder.toPath().resolve("database").resolve("sqlite.db").absolute()
    private val lock = Semaphore(1)

    init {
        try {
            if(dbFile.notExists()) {
                dbFile.parent?.createDirectories()
                dbFile.createFile()
            }
        } catch (e: IOException) {
            logger.severe("ERROR: Could not create folder ${dbFile.parent} for database.db file\n${e.stackTraceToString()}")
            Bukkit.getPluginManager().disablePlugin(plugin)
        }
    }

    private val url = "jdbc:sqlite:${dbFile.absolutePathString()}"
    private val ds = HikariDataSource(hikariConfig.apply { jdbcUrl = url })

    init { initialize() }

    fun close() = runBlocking {
        lock.withPermit { ds.safeClose() }
    }

    private fun initialize() {
        try {
            ds.connection.use { conn: Connection ->
                conn.prepareStatement(CREATE_STORAGE_GROUPS).use { it.execute() }
                conn.commit()
                logger.fine("Initiated DB")
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while trying to connect to the database and create the tables\n${e.stackTraceToString()}")
        }
    }

    // ADD

    fun addStorageGroup(group: Group) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(INSERT_STORAGE_GROUP, lock){
                group.apply {
                    setString(1, name)
                    setString(2, owner.serialize())
                    setString(3, members.serialize())
                    setString(4, senders.serialize())
                    setString(5, receivers.serialize())
                }
                execute()
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while adding group (${group.name} (${group.owner.name})) to database\n${e.stackTraceToString()}")
        }
    }

//    fun addNewEntryForPlayerLearnedSpell(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            withStatement(INSERT_LEARNED_SPELLS, lock) {
//                setString(1, playerUuid.toString())
//                setString(2, emptySet<SpellType>().toJson())
//                execute()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to add new empty entry for player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) to the database\n${e.stackTraceToString()}")
//        }
//    }
//
//    // REMOVE
//
//    fun removeSpellteacher(blockLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            withStatement(REMOVE_SPELLTEACHER) {
//                setString(1, blockLoc.toJson())
//                execute()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to remove a Spellteacher from the database\n${e.stackTraceToString()}")
//        }
//    }
//
//    private fun removeSpellteachersByWorldUuid(worldUuids: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            withStatement(REMOVE_SPELLTEACHER_OF_WORLD) {
//                worldUuids.forEach {
//                    setString(1, "%$it%")
//                    addBatch()
//                }
//                executeBatch()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to buck remove all Spellteachers from worlds\n${e.stackTraceToString()}")
//        }
//    }
//
//    private fun removeSpellteachersByLocation(locations: Iterable<Location>) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            withStatement(REMOVE_SPELLTEACHER) {
//                locations.forEach { loc ->
//                    setString(1, loc.toJson())
//                    addBatch()
//                }
//                executeBatch()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to remove a list of Spellteachers\n${e.stackTraceToString()}")
//        }
//    }
//
//    fun removePlayerLearnedSpells(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            withStatement(REMOVE_LEARNED_SPELLS, lock) {
//                setString(1, playerUuid.toString())
//                execute()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to remove learned spells of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database\n${e.stackTraceToString()}")
//        }
//    }

    // GET

    suspend fun getGroups(): Map<Pair<String, UUID>, Group> = withContext(Dispatchers.IO) {
        val groups = HashSet<Group>()

        try {
            withQueryStatement(SELECT_ALL_FROM_STORAGE_GROUPS) { rs ->
                while(rs.next()){
                    val name = rs.getString("group_name")
                    val owner = rs.getString("owner").toLocalPlayer()
                    val members = rs.getString("members").toLocalPlayerSet()
                    val senders = rs.getString("senders").toStorageSet()
                    val receivers = rs.getString("receivers").toStorageSet()

                    groups.add(Group(name,
                        owner,
                        members = members,
                        senders = senders,
                        receivers = receivers))
                }
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while trying to get all Spellteachers async\n${e.stackTraceToString()}")
        }
        groups.associateBy { it.mapKey }
    }

//    fun getPlayerLearnedSpells(playerUuid: UUID): MutableSet<SpellType>? {
//        try {
//            withQueryStatement(SELECT_LEARNED_SPELLS, {
//                setString(1, playerUuid.toString())
//            }) { rs ->
//                if(rs.next()) return rs.getString("known_spells").toSpellTypeSet()
//            }
//        } catch (e: SQLException) {
//            logger.severe("ERROR: An exception occurred while trying to get learned spells of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database\n${e.stackTraceToString()}")
//        }
//        return null
//    }
//
//    // UPDATE
//
    fun updateGroupMembers(group: Group) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_STORAGE_GROUP_MEMBERS, lock) {
                group.apply {
                    setString(1, members.serialize())
                    setString(2, name)
                    setString(3, owner.serialize())
                }
                execute()
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while updating group ${group.name} (owned by ${group.owner}) members to the database\n${e.stackTraceToString()}")
        }
    }

    fun updateGroupStorages(groups: Iterable<Group>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_STORAGE_GROUP_SENDERS_AND_RECEIVERS, lock) {
                groups.forEach {
                    setString(1, it.senders.serialize())
                    setString(2, it.receivers.serialize())
                    setString(3, it.name)
                    setString(4, it.owner.serialize())
                    addBatch()
                }
                executeBatch()
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while updating groups $groups senders and receivers to the database\n${e.stackTraceToString()}")
        }
    }

    fun updateGroupSenders(group: Group) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_STORAGE_GROUP_SENDERS, lock) {
                group.apply {
                    setString(1, senders.serialize())
                    setString(2, name)
                    setString(3, owner.serialize())
                }
                execute()
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while updating group $group senders to the database\n${e.stackTraceToString()}")
        }
    }

    fun updateGroupReceivers(group: Group) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_STORAGE_GROUP_RECEIVERS, lock) {
                group.apply {
                    setString(1, receivers.serialize())
                    setString(2, name)
                    setString(3, owner.serialize())
                }
                execute()
            }
        } catch (e: SQLException) {
            logger.severe("ERROR: An exception occurred while updating group $group senders to the database\n${e.stackTraceToString()}")
        }
    }

    // Serialization

    private fun LocalPlayer.serialize(): String = gson.toJson(this, LocalPlayer::class.java)

    private fun Set<LocalPlayer>.serialize(): String = gson.toJson(this, localPlayerSetTypeToken)

    @JvmName("serializeStorage")
    private fun Set<Storage>.serialize(): String = gson.toJson(this, storageSetTypeToken)

    // Deserialization

    private fun String.toLocalPlayer(): LocalPlayer = gson.fromJson(this, LocalPlayer::class.java)

    private fun String.toLocalPlayerSet(): MutableSet<LocalPlayer> = gson.fromJson(this, localPlayerSetTypeToken)

    private fun String.toStorageSet(): MutableSet<Storage> = gson.fromJson(this, storageSetTypeToken)

    private fun Regex.getOrNull(string: String, group: Int) = this.find(string)?.groupValues?.get(group)

    private fun <T> withStatement(@Language("SQL") statement: String, prepareBlock: PreparedStatement.() -> T): T {
        ds.connection.use { conn ->
            conn.prepareStatement(statement).use { prep ->
                return prep.prepareBlock().also { conn.commit() }
            }
        }
    }

    private suspend fun <T> withStatement(@Language("SQL") statement: String, semaphore: Semaphore, prepareBlock: PreparedStatement.() -> T): T {
        semaphore.withPermit {
            ds.connection.use { conn ->
                conn.prepareStatement(statement).use { prep ->
                    return prep.prepareBlock().also { conn.commit() }
                }
            }
        }
    }

    private inline fun <reified T> withQueryStatement(@Language("SQL") statement: String, noinline prepareBlock: PreparedStatement.() -> Unit = {}, resultBlock: (ResultSet) -> T): T {
        ds.connection.use { conn ->
            conn.prepareStatement(statement).use { prep ->
                prep.apply {
                    prepareBlock()
                    executeQuery().use { rs ->
                        return resultBlock(rs)
                    }
                }
            }
        }
    }

    private fun AutoCloseable?.safeClose() { runCatching { this?.close() } }

    private companion object {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .create()
        val storageSetTypeToken: Type = object : TypeToken<Set<Storage>>() {}.type
        val localPlayerSetTypeToken: Type = object : TypeToken<Set<LocalPlayer>>() {}.type
        val hikariConfig = HikariConfig().apply { isAutoCommit = false }

        // create tables
        @Language("SQL") const val CREATE_STORAGE_GROUPS = "CREATE TABLE IF NOT EXISTS storageGroups(group_name VARCHAR(40) NOT NULL, owner VARCHAR(140) NOT NULL, members VARCHAR(10000) NOT NULL, senders VARCHAR(10000) NOT NULL, receivers VARCHAR(10000) NOT NULL, PRIMARY KEY(group_name, owner));"

        // selects
        const val SELECT_ALL_FROM_STORAGE_GROUPS = "SELECT * FROM storageGroups;"
        const val SELECT_LEARNED_SPELLS= "SELECT known_spells FROM learnedSpells WHERE player_uuid = ? LIMIT 1;"
        // inserts
        const val INSERT_STORAGE_GROUP = "INSERT INTO storageGroups(group_name, owner, members, senders, receivers) VALUES (?, ?, ?, ?, ?);"
        const val INSERT_LEARNED_SPELLS = "INSERT INTO learnedSpells(player_uuid, known_spells) VALUES (?, ?);"
        // updates
        const val UPDATE_STORAGE_GROUP_MEMBERS = "UPDATE storageGroups SET members = ? WHERE group_name = ? AND owner = ?;"
        const val UPDATE_STORAGE_GROUP_SENDERS = "UPDATE storageGroups SET senders = ? WHERE group_name = ? AND owner = ?;"
        const val UPDATE_STORAGE_GROUP_RECEIVERS = "UPDATE storageGroups SET receivers = ? WHERE group_name = ? AND owner = ?;"
        const val UPDATE_STORAGE_GROUP_SENDERS_AND_RECEIVERS = "UPDATE storageGroups SET senders = ?, receivers = ? WHERE group_name = ? AND owner = ?;"
        // removes
        const val REMOVE_SPELLTEACHER_OF_WORLD = "DELETE FROM spellTeacher WHERE location LIKE ?;"
        const val REMOVE_SPELLTEACHER = "DELETE FROM spellTeacher WHERE location = ?;"
        const val REMOVE_LEARNED_SPELLS = "DELETE FROM learnedSpells WHERE player_uuid = ?;"

        val UUID_WORLD_PATTERN = """^\{"world":"([0-9a-zA-Z-]+).*""".toRegex()
    }
}

