package dev.aaa1115910.biliapi.http.util

import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun ByteArray.brotliDecompress(): ByteArray {
    val inputStream = BrotliInputStream(ByteArrayInputStream(this))
    val outputStream = ByteArrayOutputStream()
    return outputStream.use {
        val buffer = ByteArray(4096)
        var count: Int
        while (inputStream.read(buffer).also { count = it } != -1) {
            outputStream.write(buffer, 0, count)
        }
        inputStream.close()
        outputStream.toByteArray()
    }
}
