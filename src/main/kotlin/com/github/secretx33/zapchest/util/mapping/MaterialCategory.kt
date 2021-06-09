package com.github.secretx33.zapchest.util.mapping

import arrow.core.Some
import com.github.secretx33.zapchest.util.reflection.Reflections
import com.google.common.collect.ImmutableSetMultimap
import org.bukkit.Material
import java.util.EnumSet
import java.util.Locale

object MaterialCategory {

    private val nonLegacyItemMaterials = Material.values().filterTo(EnumSet.noneOf(Material::class.java)) { !it.isAir && it.isItem && !it.name.startsWith("LEGACY") }

    val categoryToMaterial: ImmutableSetMultimap<String, Material>

    fun hasCategory(category: String): Boolean = categoryToMaterial.containsKey(category.lowercase(Locale.US))

    fun getMaterials(category: String): Set<Material> = categoryToMaterial[category.lowercase(Locale.US)]

    init {
        val creativeTabGroups = nonLegacyItemMaterials.groupBy { Reflections.getCategory(it) }
            .filterKeys { it.isNotEmpty() }
            .mapKeys { (it.key as Some).value.formatNameCategory() }

        val extraGroups = ImmutableSetMultimap.builder<String, Material>().apply {

            val armorSuffixes = setOf("_helm", "_chestplate", "_boots", "_leggings")
            val armors = nonLegacyItemMaterials.filter { material -> armorSuffixes.any { material.name.endsWith(it, ignoreCase = true) } }
            putAll("armors", armors)

            val swords = nonLegacyItemMaterials.filter { it.name.endsWith("_sword") }
            putAll("swords", swords)
        }.build()

        categoryToMaterial = ImmutableSetMultimap.builder<String, Material>().apply {
            creativeTabGroups.forEach { (category, list) -> putAll(category, list) }
            putAll(extraGroups)
        }.build()
    }

    private fun String.formatNameCategory(): String {
        val sb = StringBuilder(this)
        sb.forEachIndexed { index, c ->
            if(c.isUpperCase()) {
                sb[index] = c.lowercaseChar()
                sb.insert(index, '_')
            }
        }
        return sb.toString()
    }
}
