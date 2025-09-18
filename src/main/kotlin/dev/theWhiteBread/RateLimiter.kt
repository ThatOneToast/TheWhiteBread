package dev.theWhiteBread

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class Cooldown(val nanos: Long) {
    S3(Duration.ofSeconds(3).toNanos()),
    S15(Duration.ofSeconds(15).toNanos()),
    S30(Duration.ofSeconds(30).toNanos()),
    S45(Duration.ofSeconds(45).toNanos()),
    S60(Duration.ofSeconds(60).toNanos())

    ;
}

@Suppress("UNUSED")
object RateLimiter {
    private val global = ConcurrentHashMap<Cooldown, AtomicLong>()
    private val perKey = ConcurrentHashMap<RateKey, AtomicLong>()

    private data class RateKey(val id: String, val cooldown: Cooldown)

    /**
     * Global (single) cooldown for the given [cooldown].
     * Returns true if [action] ran.
     */
    fun tryExecuteIfPassed(cooldown: Cooldown, action: () -> Unit): Boolean {
        val stamp = global.computeIfAbsent(cooldown) { AtomicLong(0L) }
        while (true) {
            val prev = stamp.get()
            val now = System.nanoTime()
            if (now - prev < cooldown.nanos) return false
            if (stamp.compareAndSet(prev, now)) {
                action()
                return true
            }
            // CAS failed -> retry
        }
    }

    /**
     * Per-key cooldown: independent cooldown per [key].
     * Returns true if [action] ran.
     */
    fun tryExecuteIfPassed(key: String, cooldown: Cooldown, action: () -> Unit):
            Boolean {
        val rk = RateKey(key, cooldown)
        val stamp = perKey.computeIfAbsent(rk) { AtomicLong(0L) }
        while (true) {
            val prev = stamp.get()
            val now = System.nanoTime()
            if (now - prev < cooldown.nanos) return false
            if (stamp.compareAndSet(prev, now)) {
                action()
                return true
            }
        }
    }

    // Convenience helpers — global
    fun tryExecuteIf3sPassed(action: () -> Unit) =
        tryExecuteIfPassed(Cooldown.S3, action)

    fun tryExecuteIf15sPassed(action: () -> Unit) =
        tryExecuteIfPassed(Cooldown.S15, action)

    fun tryExecuteIf30sPassed(action: () -> Unit) =
        tryExecuteIfPassed(Cooldown.S30, action)

    fun tryExecuteIf45sPassed(action: () -> Unit) =
        tryExecuteIfPassed(Cooldown.S45, action)

    fun tryExecuteIf60sPassed(action: () -> Unit) =
        tryExecuteIfPassed(Cooldown.S60, action)

    // Convenience helpers — per-key
    fun tryExecuteIf3sPassed(key: String, action: () -> Unit) =
        tryExecuteIfPassed(key, Cooldown.S3, action)

    fun tryExecuteIf15sPassed(key: String, action: () -> Unit) =
        tryExecuteIfPassed(key, Cooldown.S15, action)

    fun tryExecuteIf30sPassed(key: String, action: () -> Unit) =
        tryExecuteIfPassed(key, Cooldown.S30, action)

    fun tryExecuteIf45sPassed(key: String, action: () -> Unit) =
        tryExecuteIfPassed(key, Cooldown.S45, action)

    fun tryExecuteIf60sPassed(key: String, action: () -> Unit) =
        tryExecuteIfPassed(key, Cooldown.S60, action)

    fun runTicksLater(ticks: Int, action: () -> Unit) {
        TheWhiteBread.instance.server.scheduler.runTaskLater(
            TheWhiteBread.instance,
            Runnable {
                action()
            },
            ticks.toLong()
        )
    }
}