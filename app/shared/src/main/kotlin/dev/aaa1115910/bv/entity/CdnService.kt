package dev.aaa1115910.bv.entity

enum class CdnService(
    val displayName: String,
    val host: String? = null
) {
    BaseUrl("基础 URL（不推荐）"),
    BackupUrl("备用 URL"),
    Ali("ali（阿里云）", "upos-sz-mirrorali.bilivideo.com"),
    Alib("alib（阿里云）", "upos-sz-mirroralib.bilivideo.com"),
    Alio1("alio1（阿里云）", "upos-sz-mirroralio1.bilivideo.com"),
    Cos("cos（腾讯云）", "upos-sz-mirrorcos.bilivideo.com"),
    Cosb("cosb（腾讯云，VOD 加速类型）", "upos-sz-mirrorcosb.bilivideo.com"),
    Coso1("coso1（腾讯云）", "upos-sz-mirrorcoso1.bilivideo.com"),
    Hw("hw（华为云，融合 CDN）", "upos-sz-mirrorhw.bilivideo.com"),
    Hwb("hwb（华为云，融合 CDN）", "upos-sz-mirrorhwb.bilivideo.com"),
    Hwo1("hwo1（华为云，融合 CDN）", "upos-sz-mirrorhwo1.bilivideo.com"),
    Hw08c("08c（华为云，融合 CDN）", "upos-sz-mirror08c.bilivideo.com"),
    Hw08h("08h（华为云，融合 CDN）", "upos-sz-mirror08h.bilivideo.com"),
    Hw08ct("08ct（华为云，融合 CDN）", "upos-sz-mirror08ct.bilivideo.com"),
    TfHw("tf_hw（华为云）", "upos-tf-all-hw.bilivideo.com"),
    TfTx("tf_tx（腾讯云）", "upos-tf-all-tx.bilivideo.com"),
    Akamai("akamai（Akamai 海外）", "upos-hz-mirrorakam.akamaized.net"),
    Aliov("aliov（阿里云海外）", "upos-sz-mirroraliov.bilivideo.com"),
    Cosov("cosov（腾讯云海外）", "upos-sz-mirrorcosov.bilivideo.com"),
    Hwov("hwov（华为云海外）", "upos-sz-mirrorhwov.bilivideo.com"),
    HkBcache("hk_bcache（Bilibili 海外）", "cn-hk-eq-bcache-01.bilivideo.com");

    companion object {
        val Default = BackupUrl

        fun fromOrdinal(ordinal: Int): CdnService {
            return entries.getOrElse(ordinal) { Default }
        }
    }
}
