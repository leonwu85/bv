package dev.aaa1115910.biliapi.http.util

import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * 自定义 DNS 解析策略：优先返回 IPv4 地址，IPv6 地址排在后面。
 *
 * 解决在 IPv6 网络环境下，系统分配了 IPv6 地址但实际不可路由时，
 * 每次请求都要等待 IPv6 连接超时再回退到 IPv4 的问题。
 *
 * 不完全禁用 IPv6：如果只有 IPv6 地址可用，依然可以正常使用。
 */
object IPv4PreferredDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        val ipv4 = addresses.filter { it is Inet4Address }
        val ipv6 = addresses.filter { it is Inet6Address }
        return ipv4 + ipv6
    }
}
