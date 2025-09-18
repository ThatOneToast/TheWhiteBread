package dev.theWhiteBread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class ThreadingScopes(private val pluginJob: Job) {
    val pluginScope = CoroutineScope(Dispatchers.Default + pluginJob)
    val migrationDispatcher = Executors.newFixedThreadPool(
        maxOf(1, Runtime.getRuntime().availableProcessors() - 1)
    ).asCoroutineDispatcher()
    val bukkitDispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
            TheWhiteBread.instance.server.scheduler.runTask(TheWhiteBread.instance, block)
        }
    }
}