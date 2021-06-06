package com.github.secretx33.zapchest.database

import com.github.secretx33.zapchest.model.Group
import com.github.secretx33.zapchest.model.Storage
import com.github.secretx33.zapchest.util.extension.toUuid
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
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

class SQLite(plugin: Plugin, private val config: Config) {

    private val log = plugin.logger
    private val dbFile = plugin.dataFolder.resolve("database").resolve("sqlite.db").toPath().absolute()
    private val lock = Semaphore(1)

    init {
        try {
            dbFile.createDirectories()
        } catch (e: IOException) {
            log.severe("ERROR: Could not create folder ${dbFile.parent} for database.db file\n${e.stackTraceToString()}")
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
                log.fine("Initiated DB")
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to connect to the database and create the tables\n${e.stackTraceToString()}")
        }
    }

    // ADD

    fun addSpellteacher(teacher: SpellTeacher) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(INSERT_SPELLTEACHER){
                setString(1, teacher.location.toJson())
                setString(2, teacher.spellType.toJson())
                setString(3, teacher.blockMaterial.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while adding a specific Spellteacher (${teacher.location.formattedString()})\n${e.stackTraceToString()}")
        }
    }

    fun addNewEntryForPlayerLearnedSpell(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(INSERT_LEARNED_SPELLS, lock) {
                setString(1, playerUuid.toString())
                setString(2, emptySet<SpellType>().toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to add new empty entry for player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) to the database\n${e.stackTraceToString()}")
        }
    }

    // REMOVE

    fun removeSpellteacher(blockLoc: Location) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_SPELLTEACHER) {
                setString(1, blockLoc.toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove a Spellteacher from the database\n${e.stackTraceToString()}")
        }
    }

    private fun removeSpellteachersByWorldUuid(worldUuids: Iterable<String>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_SPELLTEACHER_OF_WORLD) {
                worldUuids.forEach {
                    setString(1, "%$it%")
                    addBatch()
                }
                executeBatch()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to buck remove all Spellteachers from worlds\n${e.stackTraceToString()}")
        }
    }

    private fun removeSpellteachersByLocation(locations: Iterable<Location>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_SPELLTEACHER) {
                locations.forEach { loc ->
                    setString(1, loc.toJson())
                    addBatch()
                }
                executeBatch()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove a list of Spellteachers\n${e.stackTraceToString()}")
        }
    }

    fun removePlayerLearnedSpells(playerUuid: UUID) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(REMOVE_LEARNED_SPELLS, lock) {
                setString(1, playerUuid.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to remove learned spells of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database\n${e.stackTraceToString()}")
        }
    }

    // GET

    suspend fun getGroups(): Set<Group> = withContext(Dispatchers.IO) {
        val groups = HashSet<Group>()

        try {
            withQueryStatement(SELECT_ALL_FROM_STORAGE_GROUPS) { rs ->
                while(rs.next()){
                    val name = rs.getString("group_name")
                    val owner = rs.getString("owner_uuid").toUuid()
                    val members = rs.getString("member_uuids").toUuidSet()
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
            log.severe("ERROR: An exception occurred while trying to get all Spellteachers async\n${e.stackTraceToString()}")
        }
        groups
    }

    fun getPlayerLearnedSpells(playerUuid: UUID): MutableSet<SpellType>? {
        try {
            withQueryStatement(SELECT_LEARNED_SPELLS, {
                setString(1, playerUuid.toString())
            }) { rs ->
                if(rs.next()) return rs.getString("known_spells").toSpellTypeSet()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to get learned spells of player ${Bukkit.getPlayer(playerUuid)?.name} ($playerUuid) from database\n${e.stackTraceToString()}")
        }
        return null
    }

    // UPDATE

    fun updateSpellteacher(newTeacher: SpellTeacher) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_SPELLTEACHER) {
                setString(1, newTeacher.spellType.toJson())
                setString(2, newTeacher.location.toJson())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while updating Spellteacher at ${newTeacher.location.formattedString()} to type ${newTeacher.spellType} to the database\n${e.stackTraceToString()}")
        }
    }

    fun updatePlayerLearnedSpells(playerUuid: UUID, knownSpells: Set<SpellType>) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withStatement(UPDATE_LEARNED_SPELLS, lock) {
                setString(1, knownSpells.toJson())
                setString(2, playerUuid.toString())
                execute()
            }
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to add player ${Bukkit.getPlayer(playerUuid)?.name ?: "Unknown"} ($playerUuid) knownSpells (${knownSpells.joinToString()}) to the database\n${e.stackTraceToString()}")
        }
    }

    private fun String.toStorageSet(): MutableSet<Storage> = gson.fromJson(this, storageSetTypeToken)

    private fun String.toUuidSet(): MutableSet<UUID> = gson.fromJson(this, uuidSetTypeToken)

    private fun Location.toJson() = gson.toJson(this, Location::class.java)

    private fun String.toSpellType() = gson.fromJson(this, SpellType::class.java)

    private fun SpellType.toJson() = gson.toJson(this, SpellType::class.java)

    private fun Set<SpellType>.toJson() = gson.toJson(this, groupSetTypeToken)

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
        val groupSetTypeToken: Type = object : TypeToken<Set<Group>>() {}.type
        val storageSetTypeToken: Type = object : TypeToken<Set<Storage>>() {}.type
        val uuidSetTypeToken: Type = object : TypeToken<Set<UUID>>() {}.type
        val hikariConfig = HikariConfig().apply { isAutoCommit = false }

        // create tables
        @Language("SQL") const val CREATE_STORAGE_GROUPS = "CREATE TABLE IF NOT EXISTS storageGroups(group_name VARCHAR(100) NOT NULL PRIMARY KEY, owner_uuid VARCHAR(60) NOT NULL PRIMARY KEY, member_uuids VARCHAR(10000) NOT NULL, senders VARCHAR(10000) NOT NULL, receivers VARCHAR(10000) NOT NULL);"

        // selects
        const val SELECT_ALL_FROM_STORAGE_GROUPS = "SELECT * FROM storageGroups;"
        const val SELECT_LEARNED_SPELLS= "SELECT known_spells FROM learnedSpells WHERE player_uuid = ? LIMIT 1;"
        // inserts
        const val INSERT_SPELLTEACHER = "INSERT INTO spellTeacher(location, spell_type, block_material) VALUES (?, ?, ?);"
        const val INSERT_LEARNED_SPELLS = "INSERT INTO learnedSpells(player_uuid, known_spells) VALUES (?, ?);"
        // updates
        const val UPDATE_SPELLTEACHER = "UPDATE spellTeacher SET spell_type = ? WHERE location = ?;"
        const val UPDATE_LEARNED_SPELLS = "UPDATE learnedSpells SET known_spells = ? WHERE player_uuid = ?;"
        // removes
        const val REMOVE_SPELLTEACHER_OF_WORLD = "DELETE FROM spellTeacher WHERE location LIKE ?;"
        const val REMOVE_SPELLTEACHER = "DELETE FROM spellTeacher WHERE location = ?;"
        const val REMOVE_LEARNED_SPELLS = "DELETE FROM learnedSpells WHERE player_uuid = ?;"

        val UUID_WORLD_PATTERN = """^\{"world":"([0-9a-zA-Z-]+).*""".toRegex()
    }
}

