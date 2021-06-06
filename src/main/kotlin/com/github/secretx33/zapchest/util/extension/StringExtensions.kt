package com.github.secretx33.zapchest.util.extension

import org.apache.commons.lang.WordUtils
import java.util.UUID

fun String.capitalizeFully(): String = WordUtils.capitalizeFully(this)

fun String.toUuid(): UUID = UUID.fromString(this)
