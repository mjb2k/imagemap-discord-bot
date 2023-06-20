package com.mjb2k.imagemapbot

/* imports for imagemaps */

import com.mjb2k.imagemapbot.exceptions.MissingIntentsException
import discord4j.common.close.CloseException
import discord4j.common.store.Store
import discord4j.common.store.impl.LocalStoreLayout
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Message
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.util.AllowedMentions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.outputStream

class Client(private val plugin: ImageMapBot) {

    private var gateway: GatewayDiscordClient? = null

    // function for imagemaps downloading
    private fun downloadFile(url: URL): Path {
        val file = kotlin.io.path.createTempFile()
        url.openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    suspend fun connect(token: String) {
        val client = DiscordClient.create(token)
        try {
            gateway = client
                .gateway()
                .setStore(Store.fromLayout(LocalStoreLayout.create()))
                .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES))
                .login()
                .awaitFirstOrNull() ?: throw Exception("Failed to connect to Discord")
        } catch (error: CloseException) {
            if (error.closeStatus.code == 4014) throw MissingIntentsException(client.applicationId.awaitFirst())
            throw error
        }
    }

    suspend fun disconnect() {
        gateway?.apply {
            logout().awaitFirstOrNull()
            eventDispatcher.shutdown()
        }
        gateway = null
    }

    suspend fun initListeners() = coroutineScope {
        gateway?.apply {
            awaitAll(
                async {
                    eventDispatcher
                        .on(MessageCreateEvent::class.java)
                        .asFlow()
                        .collect {
                            val imageMapChannels = plugin.configManager.imagemaps.channels.map(Snowflake::of)
                            val channelId = it.message.channelId
                            when {
                                imageMapChannels.contains(channelId) -> onSyncedMessage(it.message)

                                else -> messagesDebug(
                                    "Ignoring message ${it.message.id.asString()}, channel ${
                                        it.message.channelId.asString()
                                    } not configured in chat.channels or chat.console-channels"
                                )
                            }
                        }
                }
            )
        }
    }

    private fun messagesDebug(log: String) {
        if (!plugin.configManager.imagemaps.debug) return
        plugin.logger.info(log)
    }

    private suspend fun onSyncedMessage(message: Message) {
        messagesDebug("Received message ${message.id.asString()} on channel ${message.channelId.asString()}")
        when {
            !message.author.isPresent -> messagesDebug("Ignoring message, cannot get message author")
            message.author.get().isBot -> messagesDebug("Ignoring message, author is a bot")
            message.content.isNullOrEmpty() && message.attachments.isEmpty() -> messagesDebug("Ignoring message, content empty")
            /* Here is where we'll check for messages containing files */
            message.attachments.isNotEmpty() -> {
                // verify imageMap integration is enabled
                /* verify this message was sent in a imchannel */
                if (!plugin.configManager.imagemaps.channels.map(Snowflake::of).contains(message.channelId)) {
                    messagesDebug("Ignoring attachment, not part of one of the imagemap channels")
                    return
                }

                // communicate that we're processing players file
                if (plugin.configManager.imagemaps.reply) {
                    message.channel.awaitFirstOrNull()?.let {
                        it.createMessage(
                            "Processing attachments %s".format(message.author.get().mention)
                        ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                    }?.awaitFirstOrNull()
                }


                /* Go through each attachment, download and scan for image */
                for (attachment in message.attachments) {
                    /* gets the URL of the attachment for download */
                    var url = attachment.url
                    var filename = attachment.filename.lowercase().replace(" ", "_")
                    var pathToFile = downloadFile(URL(url))

                    /* if not a PNG file then we stop execution */
                    if (!plugin.fileScanner.scan(pathToFile)) {
                        // communicate with player that this attachement is not a PNG or too large
                        if (plugin.configManager.imagemaps.reply) {
                            message.channel.awaitFirstOrNull()?.let {
                                it.createMessage(
                                    "This is not a PNG image, or is too large!! %s %s".format(message.author.get().mention, filename)
                                ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                            }?.awaitFirstOrNull()
                        }
                        messagesDebug("This is not a PNG file!")
                        return
                    }

                    /* if no issue with file, attempt migration */
                    if(!plugin.imageMapMigrator
                            .migrateImage(pathToFile, filename, plugin.configManager.imagemaps.path)) {
                        message.channel.awaitFirstOrNull()?.let {
                            it.
                            createMessage(
                                "Upload failed for file: %s %s, check logs".format(filename, message.author.get().mention)
                            ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                        }?.awaitFirstOrNull()
                    }
                    else {
                        // communicate upload complete, use /imagemaps place <filename>
                        if (plugin.configManager.imagemaps.reply) {
                            message.channel.awaitFirstOrNull()?.let {
                                it.createMessage(
                                    "Upload complete, you may use '/imagemap place %s' to place your image %s".format(
                                        filename,
                                        message.author.get().mention)
                                ).withAllowedMentions(AllowedMentions.builder().allowUser(message.author.get().id).build())
                            }?.awaitFirstOrNull()
                        }
                    }
                }
            }
        }
    }


}