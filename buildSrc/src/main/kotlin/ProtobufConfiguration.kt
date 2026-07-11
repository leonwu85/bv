import java.io.File

object ProtobufConfiguration {
    val usedProtoFiles = setOf(
        "bilibili/app/archive/middleware/v1/preload.proto",
        "bilibili/app/archive/v1/archive.proto",
        "bilibili/app/card/v1/ad.proto",
        "bilibili/app/card/v1/card.proto",
        "bilibili/app/card/v1/common.proto",
        "bilibili/app/card/v1/single.proto",
        "bilibili/app/dynamic/v2/dynamic.proto",
        "bilibili/app/im/v1/im.proto",
        "bilibili/app/interfaces/v1/history.proto",
        "bilibili/app/interfaces/v1/search.proto",
        "bilibili/app/playeronline/v1/playeronline.proto",
        "bilibili/app/playerunite/v1/playerunite.proto",
        "bilibili/app/show/popular/v1/popular.proto",
        "bilibili/app/view/v1/view.proto",
        "bilibili/app/viewunite/common.proto",
        "bilibili/community/service/dm/v1/dm.proto",
        "bilibili/dagw/component/avatar/common/common.proto",
        "bilibili/dagw/component/avatar/v1/avatar.proto",
        "bilibili/dagw/component/avatar/v1/plugin.proto",
        "bilibili/im/interfaces/v1/im.proto",
        "bilibili/im/type/im.proto",
        "bilibili/main/community/reply/v1/reply.proto",
        "bilibili/metadata/device/device.proto",
        "bilibili/metadata/locale/locale.proto",
        "bilibili/metadata/metadata.proto",
        "bilibili/metadata/network/network.proto",
        "bilibili/pagination/pagination.proto",
        "bilibili/pgc/gateway/player/v2/playurl.proto",
        "bilibili/playershared/playershared.proto",
        "bilibili/polymer/app/search/v1/search.proto",
        "bilibili/rpc/status.proto",
        "common/ErrorProto.proto",
    )
    fun validateProtoFiles(rootDir: File) {
        require(rootDir.isDirectory) {
            "Proto source directory does not exist: ${rootDir.absolutePath}"
        }
        val missingProtoFiles = usedProtoFiles.filterNot { rootDir.resolve(it).isFile }
        require(missingProtoFiles.isEmpty()) {
            "Configured proto files do not exist: ${missingProtoFiles.sorted().joinToString()}"
        }
    }
}
