package dev.aaa1115910.bv.util

fun String.resizedImageUrl(size: ImageSize): String {
    return when (size) {
        ImageSize.Default -> this
        else -> "$this@${size.sizeString}.webp"
    }
}

enum class ImageSize(val sizeString: String) {
    Default(""),
    Cover("180h_288w_1c"),
    TvEpisodeCover("360h_640w_1c"),
    SmallVideoCardCover("400h_640w_1c"),
    SeasonCoverThumbnail("466h_622w"),
    LargeCover("480h_768w_1c"),
    DynamicPreview("320h_640w_1c"),
    DynamicDetailSmall("320w"),
    DynamicDetailPreview("540w"),
    DynamicDetailLarge("720w"),
    DynamicLongDetailPreview("480w"),
    DynamicLongDetailLarge("640w"),
    Icon("100h_100w_1c")
}
