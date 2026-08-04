package dev.aaa1115910.bv.player

import androidx.core.util.Pools
import kotlin.test.Test
import kotlin.test.assertIs

class AkDanmakuObjectPoolTest {
    @Test
    fun `vendored pools are synchronized for render thread access`() {
        val objectPoolClass = Class.forName("com.kuaishou.akdanmaku.utils.ObjectPool")

        listOf("rectPool", "pointPool", "itemPool").forEach { fieldName ->
            val pool = objectPoolClass.getDeclaredField(fieldName).run {
                isAccessible = true
                get(null)
            }

            assertIs<Pools.SynchronizedPool<*>>(pool, "$fieldName must be thread-safe")
        }
    }
}
