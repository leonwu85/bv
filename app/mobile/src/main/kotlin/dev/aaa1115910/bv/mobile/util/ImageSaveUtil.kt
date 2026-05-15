package dev.aaa1115910.bv.mobile.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SavedImageType(
    val mimeType: String,
    val extension: String
)

suspend fun saveImageToGallery(
    context: Context,
    imageUrl: String
): String = withContext(Dispatchers.IO) {
    require(imageUrl.isNotBlank()) { "图片地址为空" }

    val bytes = BiliHttpApi.download(imageUrl)
    val imageType = detectImageType(bytes, imageUrl)
    val displayName = "BV_${System.currentTimeMillis()}.${imageType.extension}"
    val resolver = context.applicationContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, imageType.mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/BV")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("无法创建图片文件")
    try {
        resolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
        } ?: error("无法写入图片文件")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                },
                null,
                null
            )
        }
    } catch (throwable: Throwable) {
        resolver.delete(uri, null, null)
        throw throwable
    }
    displayName
}

private fun detectImageType(
    bytes: ByteArray,
    imageUrl: String
): SavedImageType {
    if (bytes.matchesMagic(0xff, 0xd8, 0xff)) {
        return SavedImageType(mimeType = "image/jpeg", extension = "jpg")
    }
    if (bytes.matchesMagic(0x89, 0x50, 0x4e, 0x47)) {
        return SavedImageType(mimeType = "image/png", extension = "png")
    }
    if (bytes.matchesMagic(0x47, 0x49, 0x46)) {
        return SavedImageType(mimeType = "image/gif", extension = "gif")
    }
    if (
        bytes.matchesMagic(0x52, 0x49, 0x46, 0x46) &&
        bytes.size >= 12 &&
        (bytes[8].toInt() and 0xff) == 0x57 &&
        (bytes[9].toInt() and 0xff) == 0x45 &&
        (bytes[10].toInt() and 0xff) == 0x42 &&
        (bytes[11].toInt() and 0xff) == 0x50
    ) {
        return SavedImageType(mimeType = "image/webp", extension = "webp")
    }

    val extension = Regex(
        pattern = """\.(jpe?g|png|webp|gif)(?=($|[?#@]))""",
        option = RegexOption.IGNORE_CASE
    ).find(imageUrl)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()
        ?.let { if (it == "jpeg") "jpg" else it }

    return when (extension) {
        "png" -> SavedImageType(mimeType = "image/png", extension = "png")
        "gif" -> SavedImageType(mimeType = "image/gif", extension = "gif")
        "webp" -> SavedImageType(mimeType = "image/webp", extension = "webp")
        else -> SavedImageType(mimeType = "image/jpeg", extension = "jpg")
    }
}

private fun ByteArray.matchesMagic(vararg bytes: Int): Boolean {
    if (size < bytes.size) return false
    return bytes.indices.all { index ->
        (this[index].toInt() and 0xff) == bytes[index]
    }
}
