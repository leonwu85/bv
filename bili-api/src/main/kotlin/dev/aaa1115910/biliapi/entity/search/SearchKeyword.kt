package dev.aaa1115910.biliapi.entity.search

data class SearchKeyword(
    val keyword: String,
    val showName: String = keyword,
    val icon: String? = null,
    val showLiveIcon: Boolean = false,
    val recommendReason: String? = null
) {
    val displayName: String
        get() = showName.ifBlank { keyword }

    companion object {
        fun fromHttpWebHotword(hotword: dev.aaa1115910.biliapi.http.entity.search.Hotword) =
            SearchKeyword(
                keyword = hotword.keyword,
                showName = hotword.showName,
                icon = hotword.icon.takeIf { it.isNotBlank() }
            )

        fun fromHttpAppSquareDataItem(squareDataItem: dev.aaa1115910.biliapi.http.entity.search.AppSearchSquareData.SquareData.SquareDataItem) =
            SearchKeyword(
                keyword = squareDataItem.keyword.orEmpty(),
                showName = squareDataItem.showName.orEmpty(),
                icon = squareDataItem.icon?.takeIf { it.isNotBlank() },
                showLiveIcon = squareDataItem.showLiveIcon == true
            )

        fun fromHttpAppSearchTrendingHotword(hotword: dev.aaa1115910.biliapi.http.entity.search.SearchTendingData.Hotword) =
            SearchKeyword(
                keyword = hotword.keyword,
                showName = hotword.showName,
                icon = hotword.icon?.takeIf { it.isNotBlank() }
            )

        fun fromHttpSearchRecommendItem(item: dev.aaa1115910.biliapi.http.entity.search.SearchRecommendData.Item) =
            SearchKeyword(
                keyword = item.keyword.orEmpty(),
                icon = item.icon?.takeIf { it.isNotBlank() },
                showLiveIcon = item.showLiveIcon == true,
                recommendReason = item.recommendReason
                    ?.replaceFirst('·', ' ')
                    ?.takeIf { it.isNotBlank() }
            )
    }
}
