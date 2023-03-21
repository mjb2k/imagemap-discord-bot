package com.mjb2k.imagemapbot.exceptions

// thank you Dominik Korsa for this little exception
class ConfigNotSetException(route: String) : Exception("Field $route not set") {
    constructor(parent: String?, route: String) : this(parent?.let { "$it.$route" } ?: route)
}
