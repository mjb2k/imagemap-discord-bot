package com.mjb2k.imagemapbot.imagemaps

import com.mjb2k.imagemapbot.ImageMapBot
import java.io.File
import java.io.IOException
import java.nio.file.Path
import javax.imageio.ImageIO

/* This class will scan files submitted by the user to make sure
* they are actually PNG files and are safe. */
class FileScanner(private val plugin: ImageMapBot) {


    /* Call verifyPNG and verifySafeness*/
     fun scan(file: Path): Boolean {
        return verifyPNG(file.toFile())
    }

    private fun verifyPNG(file: File): Boolean {
        try {
            // verify it's an image, and then verify it has .PNG extension
            checkFileSize(file, 256000000) // 256MB limit
            ImageIO.read(file).toString()
        } catch (e: Exception) {
            plugin.logger.warning(e.toString())
            return false
        }
        return true
    }

    private fun checkFileSize(file: File, maxSizeInBytes: Long) {
        val fileSize = file.length()
        if (fileSize > maxSizeInBytes) {
            throw IOException("File size (${fileSize} bytes) exceeds the limit (${maxSizeInBytes} bytes)")
        }
    }
}