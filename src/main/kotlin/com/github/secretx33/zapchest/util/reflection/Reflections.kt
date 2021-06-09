package com.github.secretx33.zapchest.util.reflection

import arrow.core.None
import arrow.core.Option
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Base64
import java.util.Locale
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap


object Reflections {

    private val gson = Gson()
    private val jsonParser = JsonParser()
    private val version: String = Bukkit.getServer().javaClass.getPackage().name.replace(".", ",").split(",").toTypedArray()[3]
    private val CraftEntity: Class<*> = getBukkitClass("entity.CraftEntity")
    private val CraftLivingEntity: Class<*> = getBukkitClass("entity.CraftLivingEntity")
    private val CraftEntityEquipment: Class<*> = getBukkitClass("inventory.CraftEntityEquipment")
    private val CraftItemStack: Class<*> = getBukkitClass("inventory.CraftItemStack")
    private val CraftMagicNumbers: Class<*> = getBukkitClass("util.CraftMagicNumbers")
    private val NMS_Entity: Class<*> = getNMSClass("Entity")
    private val NMS_Item: Class<*> = getNMSClass("Item")
    private val NMS_ItemStack: Class<*> = getNMSClass("ItemStack")
    private val NMS_Block: Class<*> = getNMSClass("Block")
    private val NMS_Blocks: Class<*> = getNMSClass("Blocks")
    private val NMS_CreativeModeTab: Class<*> = getNMSClass("CreativeModeTab")
    private val NBTBase: Class<*> = getNMSClass("NBTBase")
    private val NBTTagCompound: Class<*> = getNMSClass("NBTTagCompound")
    private val NBTCompressedStreamTools: Class<*> = getNMSClass("NBTCompressedStreamTools")

    private fun getBukkitClass(bukkitClassString: String): Class<*> {
        val name = "org.bukkit.craftbukkit.$version.$bukkitClassString"
        return Class.forName(name)
    }

    private fun getNMSClass(nmsClassString: String): Class<*> {
        val name = "net.minecraft.server.$version.$nmsClassString"
        return Class.forName(name)
    }

    private val fields       = ConcurrentHashMap<Pair<String, String>, Field>()
    private val methods      = ConcurrentHashMap<Triple<String, String, Array<out Class<*>?>>, Method>()
    private val constructors = ConcurrentHashMap<Pair<String, Array<out Class<*>?>>, Constructor<*>>()

    private fun Class<*>.field(fieldName: String): Field {
        val className = this::class.java.name.toString()

        return this@Reflections.fields.getOrPut(Pair(className, fieldName)) {
            this.getDeclaredField(fieldName).apply {
                isAccessible = true
            }
        }
    }

    private fun Class<*>.method(methodName: String, vararg parameterTypes: Class<*>?): Method {
        val className = this::class.java.name.toString()

        return this@Reflections.methods.getOrPut(Triple(className, methodName, parameterTypes)) {
            this.getDeclaredMethod(methodName, *parameterTypes).apply {
                isAccessible = true
            }
        }
    }

    private fun Class<*>.constructor(vararg parameterTypes: Class<*>?): Constructor<*> {
        val className = this::class.java.name.toString()

        return this@Reflections.constructors.getOrPut(Pair(className, parameterTypes)) {
            this.getDeclaredConstructor(*parameterTypes).apply {
                isAccessible = true
            }
        }
    }

    fun getCategory(material: Material): Option<String> {
        val nmsItem = CraftMagicNumbers.method("getItem", Material::class.java).invoke(null, material)
        val craftCategory = NMS_Item.declaredFields.first { NMS_CreativeModeTab.isAssignableFrom(it.type) }.apply { isAccessible = true }.get(nmsItem) ?: return None
        val categoryName = NMS_CreativeModeTab.declaredFields.first { it.type == String::class.java && Modifier.isFinal(it.modifiers) && Modifier.isPrivate(it.modifiers) }.apply { isAccessible = true }.get(craftCategory)
        return Option.fromNullable(categoryName as? String)
    }

    fun getNMSEntity(entity: Entity): Any? {
        try {
            return CraftEntity.cast(entity).javaClass.method("getHandle").invoke(entity)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun clone(stack: ItemStack): ItemStack {
        return CraftItemStack.method("asCraftCopy", ItemStack::class.java).invoke(CraftItemStack, stack) as ItemStack
    }

    fun getCraftItemStack(stack: ItemStack): Any? {
        if (CraftItemStack.isInstance(stack)) return CraftItemStack.cast(stack)
        try {
            return CraftItemStack.method("asCraftCopy", ItemStack::class.java).invoke(CraftItemStack, stack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getNMSItemStack(item: Any): Any? {
        if (NMS_ItemStack.isInstance(item)) return NMS_ItemStack.cast(item)
        val itemStack = item as ItemStack
        try {
            return CraftItemStack.method("asNMSCopy", ItemStack::class.java).invoke(CraftItemStack, itemStack)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun setValueOnFinalField(field: Field, `object`: Any, newValue: Any) {
        field.isAccessible = true
        try {
            Field::class.java.field("modifiers").setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[`object`] = newValue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNbtTagFromInputStream(inputStream: InputStream): Any? {
        try {
            val readTag_NBTCompStreamTools = NBTCompressedStreamTools.method("a", InputStream::class.java)
            return readTag_NBTCompStreamTools.invoke(NBTCompressedStreamTools, inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun writeNbtTagToByteArray(nbtTag: Any): ByteArray {
        ByteArrayOutputStream().use { byteOS ->
            DataOutputStream(byteOS).use { dataOS ->
                val readTag_NBTCompStreamTools = NBTCompressedStreamTools.method("a", NBTTagCompound, OutputStream::class.java)
                readTag_NBTCompStreamTools.invoke(NBTCompressedStreamTools, nbtTag, dataOS)
                return byteOS.toByteArray()
            }
        }
    }

    fun getNbtTag(stack: ItemStack): Any {
        try {
            val nbtTag = NBTTagCompound.newInstance()
            val nmsItem = CraftItemStack.method("asNMSCopy", ItemStack::class.java).invoke(getCraftItemStack(stack), stack)
            NMS_ItemStack.method("save", NBTTagCompound).invoke(nmsItem, nbtTag)
            return nbtTag
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Any()
    }

    fun getNbtTag(entity: Entity): Any {
        try {
            var nbtTag = NBTTagCompound.newInstance()
            val nmsEntity = getNMSEntity(entity)
            val save_nmsEntity = NMS_Entity.method("save", NBTTagCompound)
            nbtTag = save_nmsEntity.invoke(nmsEntity, nbtTag)
            return nbtTag
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Any()
    }

    fun serializeEntityNbtTag(entity: Entity): String {
        try {
            val nbtTag = getNbtTag(entity)
            val byteNbtTag = writeNbtTagToByteArray(nbtTag)
            val serializedNbtTag = gson.toJson(byteNbtTag)
            if (serializedNbtTag != null) return serializedNbtTag
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun deserializeNbtTag(stringNbtTag: String?): Any {
        val inputStream = ByteArrayInputStream(gson.fromJson(stringNbtTag, object : TypeToken<ByteArray>() {}.type))
        try {
            inputStream.use { stream ->
                val nbtTag = getNbtTagFromInputStream(stream)
                if (nbtTag != null) return nbtTag
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Any()
    }

    fun setEntityNBT(entity: Entity, newTag: Any?) {
        if (newTag == null) return
        try {
            val oldTag = getNbtTag(entity)
            val get_oldTag = NBTTagCompound.method("get", String::class.java)

            // Copy the position on the old tag to the new tag
            val set_newTag = NBTTagCompound.method("set", String::class.java, NBTBase)
            set_newTag.invoke(newTag, "Pos", get_oldTag.invoke(oldTag, "Pos"))
            // And set the DeathTime property to zero, this makes possible to keep spawning a death mob and re-grabbing it indefinitely
            val setShort_newTag = NBTTagCompound.method("setShort", String::class.java, Short::class.javaPrimitiveType)
            setShort_newTag.invoke(newTag, "DeathTime", 0.toShort())

            // And load it on the entity
            val nmsEntity = getNMSEntity(entity)
            val load_nmsEntity = NMS_Entity.method("load", NBTTagCompound)
            load_nmsEntity.invoke(nmsEntity, newTag)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createEquipmentForEntity(entity: LivingEntity): EntityEquipment? {
        try {
            val craftEntity = CraftLivingEntity.cast(entity)
            val newEntityEquipment = CraftEntityEquipment.constructor(CraftLivingEntity).newInstance(craftEntity)
            return newEntityEquipment as EntityEquipment
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun setEquipmentOnEntity(entity: LivingEntity, equipment: EntityEquipment) {
        try {
            val craftEntity = CraftLivingEntity.cast(entity)
            val entityEquipment = CraftLivingEntity.field("equipment")

            val craftEquipment = CraftEntityEquipment.cast(equipment)
            val entityOnEquip = CraftEntityEquipment.field("entity")
            setValueOnFinalField(entityOnEquip, craftEquipment, craftEntity)

            entityEquipment[craftEntity] = craftEquipment
            if (entity is Player) entity.updateInventory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Serialize a Entity into a JsonObject. It's currently not possible to serialize a Player.
     *
     * @param entity The Entity to serialize
     * @return The Entity serialized as a JsonObject
     */
    private fun serializeToJsonObject(entity: Entity): JsonObject {
        if (entity is Player) {
            throw UnsupportedOperationException("It's not possible to serialize a player")
        }

        val root = JsonObject()
        if (entity is LivingEntity) {

            if (entity.equipment != null) {
                root.addProperty("equip", true)

                EquipmentSlot.values().forEach { type ->
                    entity.equipment?.getItem(type)?.takeIf { it.type != Material.AIR }?.let { item ->
                        root.addProperty("item-" + type.name.lowercase(Locale.US), serialize(item))
                    }
                }
            } else {
                root.addProperty("equip", false)
            }
        }
        root.addProperty("type", entity.type.name)
        root.addProperty("nbttag", serializeEntityNbtTag(entity))
        return root
    }

    /**
     * Spawn a Entity in a desired Location with the given info serialized. The given string
     * will be constructed into a JsonObject to use a reference to the serializedEntity.
     *
     * @param location         Where the entity should be spawned
     * @param serializedEntity The serialized Entity
     * @return The Entity spawned
     */
    @Throws(IllegalStateException::class)
    fun spawnEntity(location: Location, serializedEntity: String?): Entity {
        return spawnEntity(location, jsonParser.parse(serializedEntity).asJsonObject)
    }

    /**
     * Spawn a Entity in a desired Location with the given info serialized.
     *
     * @param location         Where the entity should be spawned
     * @param serializedEntity The serialized Entity
     * @return The Entity spawned
     */
    @Throws(IllegalStateException::class)
    fun spawnEntity(location: Location, serializedEntity: JsonObject): Entity {
        if (!serializedEntity.has("type") || serializedEntity["type"].asString.isNullOrBlank()) {
            throw IllegalArgumentException("The type of the entity cannot be determined")
        }
        val world = location.world ?: throw NullPointerException("The world of the location is not defined")
        val type = EntityType.valueOf(serializedEntity["type"].asString)
        val entity = world.spawnEntity(location, type)

        if (serializedEntity.has("nbttag") && serializedEntity["nbttag"].asString.isNotBlank()) {
            val nbtTag = deserializeNbtTag(serializedEntity["nbttag"].asString)
            setEntityNBT(entity, nbtTag)
        }
        if (entity is LivingEntity && serializedEntity.has("equip")) {
            val hadEquip = serializedEntity["equip"].asBoolean
            if (hadEquip) {
                entity.equipment?.let { equip ->
                    for (slot in EquipmentSlot.values()) {
                        val keyName = "item-" + slot.name.lowercase(Locale.US)
                        if (serializedEntity.has(keyName)) {
                            deserializeItem(serializedEntity[keyName].asString)?.let { item -> equip.setItem(slot, item) }
                        } else {
                            equip.setItem(slot, null)
                        }
                    }
                }
            }
        }
        return entity
    }

    /**
     * Serialize a ItemStack into a String.
     *
     * @param stack The ItemStack to serialize
     * @return The serialization string
     */
    fun serialize(stack: ItemStack): String {
        val nbtTag = getNbtTag(stack)
        val byteNbtTag = writeNbtTagToByteArray(nbtTag)
        return Base64.getEncoder().withoutPadding().encodeToString(byteNbtTag)
    }

    /**
     * Serialize a Entity into a String.
     *
     * @param entity The Entity to serialize
     * @return The serialization string
     */
    fun serialize(entity: Entity): String {
        return serializeToJsonObject(entity).toString()
    }

    /**
     * Deserialize a String back to an ItemStack.
     *
     * @param serializedItem The serialized ItemStack
     * @return The deserialized ItemStack object
     */
    fun deserializeItem(serializedItem: String): ItemStack? {
        val inputStream = DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(serializedItem)))
        try {
            val nbtTag = getNbtTagFromInputStream(inputStream)

            // Creating a new NMS_ItemStack from the nbtTag
            val nmsItem: Any
            when {
                VersionUtil.serverVersion.isLowerThanOrEqualTo(VersionUtil.v1_10_2_R01) -> {
                    nmsItem = NMS_ItemStack.method("createStack", NBTTagCompound).invoke(NMS_ItemStack, nbtTag)
                }
                VersionUtil.serverVersion.isLowerThanOrEqualTo(VersionUtil.v1_12_2_R01) -> {
                    val airBlock = NMS_Blocks.field("AIR")[NMS_Block]
                    nmsItem = NMS_ItemStack.constructor(NMS_Block).newInstance(airBlock)
                    NMS_ItemStack.method("load", NBTTagCompound).invoke(nmsItem, nbtTag)
                }
                else -> {
                    nmsItem = NMS_ItemStack.method("a", NBTTagCompound).invoke(NMS_ItemStack, nbtTag)
                }
            }

            // And returning its mirrored CraftItemStack
            val craftMirror = CraftItemStack.method("asCraftMirror", NMS_ItemStack).invoke(CraftItemStack, nmsItem)
            return craftMirror as ItemStack
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.safeClose()
        }
        return null
    }

    private fun Closeable.safeClose() { runCatching { close() } }

    // Version Util (from EssentialsX)
    private object VersionUtil {
        val v1_8_8_R01  = BukkitVersion.fromString("1.8.8-R0.1-SNAPSHOT")
        val v1_9_R01    = BukkitVersion.fromString("1.9-R0.1-SNAPSHOT")
        val v1_9_4_R01  = BukkitVersion.fromString("1.9.4-R0.1-SNAPSHOT")
        val v1_10_R01   = BukkitVersion.fromString("1.10-R0.1-SNAPSHOT")
        val v1_10_2_R01 = BukkitVersion.fromString("1.10.2-R0.1-SNAPSHOT")
        val v1_11_R01   = BukkitVersion.fromString("1.11-R0.1-SNAPSHOT")
        val v1_11_2_R01 = BukkitVersion.fromString("1.11.2-R0.1-SNAPSHOT")
        val v1_12_2_R01 = BukkitVersion.fromString("1.12.2-R0.1-SNAPSHOT")
        val v1_13_0_R01 = BukkitVersion.fromString("1.13.0-R0.1-SNAPSHOT")
        val v1_13_2_R01 = BukkitVersion.fromString("1.13.2-R0.1-SNAPSHOT")
        val v1_14_R01   = BukkitVersion.fromString("1.14-R0.1-SNAPSHOT")
        val v1_14_4_R01 = BukkitVersion.fromString("1.14.4-R0.1-SNAPSHOT")
        val v1_15_R01   = BukkitVersion.fromString("1.15-R0.1-SNAPSHOT")
        val v1_15_2_R01 = BukkitVersion.fromString("1.15.2-R0.1-SNAPSHOT")
        val v1_16_1_R01 = BukkitVersion.fromString("1.16.1-R0.1-SNAPSHOT")
        val v1_16_5_R01 = BukkitVersion.fromString("1.16.5-R0.1-SNAPSHOT")

        val nmsVersion: String = run {
            val name = Bukkit.getServer().javaClass.name
            val parts = name.split("\\.").toTypedArray()
            if (parts.size > 3) {
                parts[3]
            } else ""
        }
        val serverVersion = BukkitVersion.fromString(Bukkit.getServer().bukkitVersion)

        class BukkitVersion private constructor(
            val major: Int,
            val minor: Int,
            val patch: Int,
            val revision: Double,
            val prerelease: Int
        ) : Comparable<BukkitVersion> {

            fun isHigherThan(o: BukkitVersion): Boolean = compareTo(o) > 0

            fun isHigherThanOrEqualTo(o: BukkitVersion): Boolean = compareTo(o) >= 0

            fun isLowerThan(o: BukkitVersion): Boolean = compareTo(o) < 0

            fun isLowerThanOrEqualTo(o: BukkitVersion): Boolean = compareTo(o) <= 0

            override fun equals(other: Any?): Boolean {
                if (this === other) {
                    return true
                }
                if (other == null || javaClass != other.javaClass) {
                    return false
                }
                val that = other as BukkitVersion
                return major == that.major && minor == that.minor && patch == that.patch && revision == that.revision && prerelease == that.prerelease
            }

            override fun hashCode(): Int = Objects.hash(major, minor, patch, revision, prerelease)

            override fun toString(): String {
                val sb = StringBuilder("$major.$minor")
                if (patch != 0) {
                    sb.append(".").append(patch)
                }
                if (prerelease != -1) {
                    sb.append("-pre").append(prerelease)
                }
                return sb.append("-R").append(revision).toString()
            }

            override fun compareTo(other: BukkitVersion): Int = when {
                major < other.major -> -1
                major > other.major -> 1
                minor < other.minor -> -1
                minor > other.minor -> 1
                patch < other.patch -> -1
                patch > other.patch -> 1
                prerelease > other.prerelease -> -1
                prerelease < other.prerelease -> 1
                else -> revision.compareTo(other.revision)
            }

            companion object {
                private val VERSION_PATTERN = ("^(\\d+)\\.(\\d+)\\.?([0-9]*)?(?:-pre(\\d))?(?:-?R?([\\d.]+))?(?:-SNAPSHOT)?").toPattern()

                fun fromString(string: String): BukkitVersion {
                    var matcher = VERSION_PATTERN.matcher(string)
                    if (!matcher.matches()) {
                        throw IllegalArgumentException("$string is not in valid version format. e.g. 1.8.8-R0.1")
                    }
                    return from(
                        matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        if (matcher.groupCount() < 5) "" else matcher.group(5),
                        matcher.group(4)
                    )
                }

                private fun from(
                    major: String,
                    minor: String,
                    patch: String?,
                    revision: String?,
                    prerelease: String?
                ): BukkitVersion {
                    var patch = patch
                    var revision = revision
                    var prerelease = prerelease
                    if (patch == null || patch.isEmpty()) patch = "0"
                    if (revision == null || revision.isEmpty()) revision = "0"
                    if (prerelease == null || prerelease.isEmpty()) prerelease = "-1"
                    return BukkitVersion(
                        major.toInt(),
                        minor.toInt(),
                        patch.toInt(),
                        revision.toDouble(),
                        prerelease.toInt()
                    )
                }
            }
        }
    }
}
