package dev.aaa1115910.biliapi.entity.user

data class DynamicUpData(
    val liveCount: Int,
    val liveUsers: List<DynamicUpUser>,
    val upList: List<DynamicUpUser>,
    val hasMore: Boolean,
    val offset: String?
) {
    companion object {
        fun fromFollowUpData(data: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicFollowUpData): DynamicUpData {
            val liveUsers = data.liveUsers?.items.orEmpty().map { item ->
                DynamicUpUser(
                    face = item.face.orEmpty(),
                    hasUpdate = item.hasUpdate,
                    mid = item.mid,
                    uname = item.uname.orEmpty(),
                    isLive = true,
                    roomId = item.roomId,
                    title = item.title.orEmpty()
                )
            }
            val upList = data.upList?.items.orEmpty().map { item ->
                DynamicUpUser(
                    face = item.face.orEmpty(),
                    hasUpdate = item.hasUpdate,
                    mid = item.mid,
                    uname = item.uname.orEmpty()
                )
            }
            return DynamicUpData(
                liveCount = data.liveUsers?.count ?: liveUsers.size,
                liveUsers = liveUsers,
                upList = upList,
                hasMore = data.upList?.hasMore ?: false,
                offset = data.upList?.offset
            )
        }

        fun fromUpListData(data: dev.aaa1115910.biliapi.http.entity.dynamic.DynamicUpListData): DynamicUpData =
            DynamicUpData(
                liveCount = 0,
                liveUsers = emptyList(),
                upList = data.items.map { item ->
                    DynamicUpUser(
                        face = item.face.orEmpty(),
                        hasUpdate = item.hasUpdate,
                        mid = item.mid,
                        uname = item.uname.orEmpty()
                    )
                },
                hasMore = data.hasMore,
                offset = data.offset
            )
    }
}

data class DynamicUpUser(
    val face: String,
    val hasUpdate: Boolean,
    val mid: Long,
    val uname: String,
    val isLive: Boolean = false,
    val roomId: Long = 0,
    val title: String = ""
)
