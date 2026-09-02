package dev.aaa1115910.bv.tv.util

import android.os.Build
import dev.aaa1115910.bv.player.entity.SuperResolutionType

/**
 * TV 端可选的 MPV 参数集合。
 *
 * 电视盒子的 GPU（Mali-G31/G52、PowerVR GE8300 一类）跑不动 Anime4K 的 VL 档和 FSRCNNX 16 档，
 * 所以 TV 端只暴露效率档；历史上保存过“质量”档的用户在读取时被归一化到对应的效率档。
 */
object TvMpvOptions {
    /**
     * TV 端固定使用 Android GL 上下文：mpv-android 的 ffmpeg 以 `--disable-vulkan` 构建，
     * `angle` 只存在于 Windows，历史偏好里残留的这些值只会让初始化失败。
     */
    const val GPU_CONTEXT = "android"
    const val GPU_API = ""

    /** TV 端允许选择的超分档位 */
    val superResolutionChoices: List<SuperResolutionType> = listOf(
        SuperResolutionType.Disable,
        SuperResolutionType.EfficiencyAnime,
        SuperResolutionType.EfficiencyFsrcnnx,
    )

    /** `vo=gpu + hwdec=mediacodec` 零拷贝依赖 AImageReader（API 26）；更低版本只能拷贝，4K 会卡 */
    val supportsZeroCopyHwdec: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    const val LOW_API_MPV_HINT = "此设备 Android 版本较低，MPV 无法零拷贝硬解，默认清晰度将限制为 1080P60"

    fun SuperResolutionType.coerceForTv(): SuperResolutionType = when (this) {
        SuperResolutionType.QualityAnime -> SuperResolutionType.EfficiencyAnime
        SuperResolutionType.QualityFsrcnnx -> SuperResolutionType.EfficiencyFsrcnnx
        else -> this
    }
}
