package net.ririfa.yacla.ext.db.sync

import net.ririfa.yacla.ext.db.internal.DBAccessLayer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SyncDispatcher(
    private val accessLayer: DBAccessLayer
) {
    private val queue = LinkedBlockingQueue<SyncTask>()
    private val isRunning = AtomicBoolean(true)
    private val activeCount = AtomicInteger(0)
    private val lock = Object()

    init {
        Thread {
            while (isRunning.get() || queue.isNotEmpty()) {
                try {
                    val task = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    activeCount.incrementAndGet()
                    accessLayer.save(task.type as Class<Any>, task.key, task.value as Any)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (activeCount.decrementAndGet() == 0 && queue.isEmpty()) {
                        synchronized(lock) {
                            lock.notifyAll()
                        }
                    }
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

    fun shutdownAndWait() {
        isRunning.set(false)
        synchronized(lock) {
            while (activeCount.get() > 0 || queue.isNotEmpty()) {
                lock.wait()
            }
        }
    }

    data class SyncTask(
        val key: String,
        val value: Any,
        val type: Class<*>
    )
}
