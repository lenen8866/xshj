package com.sda.books.reader.util

import java.io.File

object FileUtils {
    fun getUniqueFileName(directory: File, fileName: String): File {
        var outputFile = File(directory, fileName)
        var counter = 1

        while (outputFile.exists()) {
            val nameWithoutExt = fileName.substringBeforeLast(".")
            val ext = fileName.substringAfterLast(".", "")
            val newName = "${nameWithoutExt}_${counter++}.${ext}"
            outputFile = File(directory, newName)
        }

        return outputFile
    }
}