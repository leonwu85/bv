package dev.aaa1115910.bv.player.util

import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * 自定义 DNS 解析策略
 */
object IPv4PreferredDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        val ipv4 = addresses.filter { it is Inet4Address }
        val ipv6 = addresses.filter { it is Inet6Address }
        return ipv4 + ipv6
    }
}
