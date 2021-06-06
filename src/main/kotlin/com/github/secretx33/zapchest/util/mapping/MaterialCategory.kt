package com.github.secretx33.zapchest.util.mapping

import arrow.core.Some
import com.github.secretx33.zapchest.util.reflection.Reflections
import com.google.common.collect.ImmutableMultimap
import org.bukkit.Material
import java.util.Locale

object MaterialCategory {

    val categoryToMaterial: ImmutableMultimap<String, Material>

    init {
        val materialGroups = Material.values().groupBy { Reflections.getCategory(it) }
            .filterKeys { it.isNotEmpty() }
            .mapKeys { (it.key as Some).value }

        categoryToMaterial = ImmutableMultimap.builder<String, Material>().apply {
            materialGroups.forEach { (category, list) -> putAll(category, list) }
        }.build()
    }

    val itemCategories = categoryToMaterial.keySet().map { name -> name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() } }
}
