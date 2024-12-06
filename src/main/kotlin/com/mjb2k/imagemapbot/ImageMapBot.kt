package com.mjb2k.imagemapbot

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mjb2k.imagemapbot.config.ConfigManager
import com.mjb2k.imagemapbot.exceptions.MissingIntentsException
import com.mjb2k.imagemapbot.imagemaps.FileScanner
import com.mjb2k.imagemapbot.imagemaps.ImageMapsMigrator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class ImageMapBot : JavaPlugin() {
    val client = Client(this)

    /* two object classes needed for imagemaps */
    val imageMapMigrator = ImageMapsMigrator(this)
    val fileScanner = FileScanner(this)

    /* config options */
    lateinit var configManager: ConfigManager

    private val connectionLock = Mutex()

    override fun onEnable() {
        super.onEnable()
        configManager = ConfigManager(this)

        //val metrics = Metrics(this, 1500)


        this.launch {
            connectionLock.withLock { connect() }
        }
    }

    private suspend fun connect() {
        val token = configManager.discordToken
        if (token == null) {
            logger.severe("Connection failed: 'discord-token' in config.yml has not been set")
            return
        }
        try {
            client.connect(token)
            this.launch {
                client.initListeners()
            }
            logger.log(Level.INFO,"Successfully Connected")
        } catch (error: MissingIntentsException) {
            logger.severe("Connection failed: bot lacks discord permissions to function. " +
                    "Please enabled the \"Server members intent\" and \"Message content intent\"")
        } catch (error: Exception) {
            logger.severe("Connection failed: check console.")
            error.printStackTrace()
        }
    }

    private suspend fun disconnect() {
        client.disconnect()
    }

    suspend fun reload() {
        configManager.reload()
        connectionLock.withLock {
            disconnect()
            connect()
        }
    }

}