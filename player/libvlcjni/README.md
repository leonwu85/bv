# player/libvlcjni

libvlc-android 的 Java 层（`org.videolan.libvlc.*`），源码入库，LGPL-2.1+（见 `COPYING.LIB`）。

## 为什么不用 Maven 上的 `libvlc-all`

`libvlc-all` AAR 里的 Java 类和 `libvlcjni.so` 是一一对应的：`JNI_OnLoad` 会按名字和签名查找
`Media.createXxxTrackFromNative`、`createStatsFromNative`、`MediaPlayer.createTrackDescriptionFromNative`
等方法，找不到就加载失败。VLC 3（libvlcjni-3.x）和 VLC 4（master）在这些签名上不兼容：

| | libvlcjni-3.x（3.7.x） | master（4.0.0-eapXX） |
|---|---|---|
| `IMedia.Stats` 工厂 | `int` 计数 `(IFIFIIIIIIIIIIF)` | `long` 计数 `(JFJFJJJJJJJJJJF)` |
| `IMedia.Track` 工厂 | `(codec, originalCodec, fourcc, int id, ...)` | `(String id, String name, boolean selected, codec, ...)` |
| `MediaPlayer.TrackDescription` | 有（`createTrackDescriptionFromNative`） | 无 |
| 轨道 native | `nativeGet/SetVideoTrack(int)` 等 | `nativeSelectTrack(String)`、`nativeGetTracks(int, boolean)` 等 |
| `Media.nativeGetTracks` | 无参 | `(int type)` |
| `nativeRecord` | `(String)` | `(String, boolean)` |

应用要让用户在运行时选择下载 3.7.5 还是 4.0.0-eap29，所以这里维护一份**双版本超集** Java 层：
两个版本的 `JNI_OnLoad` 需要的类、方法、字段都存在；行为差异按 `LibVLC.isVlc3()`（`majorVersion()`
是两版都导出的 native）在运行时切换。原生方法通过导出符号懒绑定，不用 `RegisterNatives`，因此声明一个
另一版本不存在的 native 是安全的，只要不去调用它。

## 来源

- 基线：`https://code.videolan.org/videolan/libvlcjni` master @ `a8d53a91`（"Bump vlc 4 version"，
  对应 Maven `libvlc-all:4.0.0-eap29`），`libvlc/src/main/java` 与 `libvlc/src/main/res`。
- VLC 3 兼容部分来自 `libvlcjni-3.x` @ `cfe024f6`（对应 `libvlc-all:3.7.5`）。
- 不包含 AAR 里的 `assets/lua`、`assets/hrtfs`（播放列表脚本与 HRTF，本应用用不到）。

## 相对上游的改动

- `LibVLC`：`loadLibraries()` 不再 `System.exit(1)`，缺库时抛 `UnsatisfiedLinkError`；新增
  `markLibrariesLoaded()`（应用用 `System.load` 从下载目录加载后调用）、`isVlc3()`；VLC 3 下补上
  `--aout` / `--android-display-chroma RV16` 默认值（3.x 行为）。
- `Media`：补回 3.x 签名的 `createXxxTrackFromNative` / `createStatsFromNative` 重载（3.x 的 int id 以
  VLC 4 风格 `"audio/3"` 暴露）；`getTracks(type)` 在 VLC 3 下从全部轨道过滤；补回 `getTrackCount()` /
  `getTrack(idx)` / `getState()`；`Parse` 常量使用 VLC 4 位布局，VLC 3 下转换；`setHWDecoderEnabled`
  恢复 3.x 的 `:codec=` 分支（`util/HWDecoderUtil` 来自 3.x）。
- `MediaPlayer`：补回 `TrackDescription` 与 3.x 轨道 native；VLC 4 的字符串 id 轨道 API 在 VLC 3 上映射到
  整数 id API；`setDisplayFit` / teletext 透明度在 VLC 3 上为空操作；`setScale`、`stop()` 后重设
  aout 的 3.x 行为按版本切换。
- `VideoHelper`：同时保留 3.x（Java 侧按视频尺寸/SAR 排布 SurfaceView）和 4.0（核心给出 place 矩形 +
  display-fit）两套布局逻辑，`setVideoLayout` 的六个参数按版本解释。

升级任一版本时：重新导出两份 `libvlcjni.so` 的 `Java_*` 符号和 JNI 查找的成员（`strings` + `javap`），
比对上表，再合并上游 Java 变更。
