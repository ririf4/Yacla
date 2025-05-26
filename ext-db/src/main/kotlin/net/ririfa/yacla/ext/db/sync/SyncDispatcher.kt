package net.ririfa.yacla.ext.db.sync

import net.ririfa.yacla.ext.db.internal.DBAccessLayer
import java.util.concurrent.LinkedBlockingQueue

class SyncDispatcher(
    private val accessLayer: DBAccessLayer
) {

    private val queue = LinkedBlockingQueue<SyncTask>()

    init {
        Thread {
            while (true) {
                try {
                    val task = queue.take()
                    @Suppress("UNCHECKED_CAST")
                    accessLayer.save(task.type as Class<Any>, task.key, task.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.apply {
            name = "Yacla-DB-Sync"
            isDaemon = true
        }.start()
    }

    fun enqueue(task: SyncTask) {
        queue.offer(task)
    }

    data class SyncTask(
        val key: String,
        val value: Any,
        val type: Class<*>
    )
}
