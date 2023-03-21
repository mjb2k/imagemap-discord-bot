package com.mjb2k.imagemapbot.config

import com.mjb2k.imagemapbot.exceptions.ConfigNotSetException
import dev.dejvokep.boostedyaml.block.implementation.Section

fun Section.requireTrimmedString(route: String) =
    getString(route)?.trimEnd() ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireBoolean(route: String) = getBoolean(route) ?: throw ConfigNotSetException(routeAsString, route)

fun Section.requireStringList(route: String): List<String> =
    getStringList(route) ?: throw ConfigNotSetException(routeAsString, route)
