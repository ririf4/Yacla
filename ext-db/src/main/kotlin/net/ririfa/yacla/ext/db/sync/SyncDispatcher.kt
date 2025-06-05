package net.ririfa.yacla.ext.db.sync

import net.ririfa.yacla.ext.db.internal.DBAccessLayer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Asynchronous sync dispatcher for DB-backed config caching.
 *
 * This class manages a background thread that consumes a queue of [SyncTask]s,
 * ensuring that config objects evicted from the in-memory cache are persisted
 * to the database without blocking the main thread.
 *
 * Typically used internally by DB-backed Yacla config loaders to provide
 * write-behind caching and eventual consistency with minimal latency.
 *
 * The dispatcher guarantees that all enqueued tasks are completed before shutdown.
 *
 * Example usage:
 * ```
 * val dispatcher = SyncDispatcher(myAccessLayer)
 * dispatcher.enqueue(SyncDispatcher.SyncTask("key", config, MyClass::class.java))
 * ...
 * dispatcher.shutdownAndWait()
 * ```
 */
class SyncDispatcher(
    private val accessLayer: DBAccessLayer
) {
    private val queue = LinkedBlockingQueue<SyncTask>()
    private val isRunning = AtomicBoolean(true)
    private val activeCount = AtomicInteger(0)
    private val lock = Object()

    init {
        // Launches the worker thread for processing sync tasks.
        Thread {
            while (isRunning.get() || queue.isNotEmpty()) {
                try {
                    val task = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    activeCount.incrementAndGet()
                    accessLayer.save(task.type as Class<Any>, task.key, task.value)
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

    /**
     * Enqueues a [SyncTask] for asynchronous saving to the database.
     *
     * @param task the sync task representing a config update
     */
    fun enqueue(task: SyncTask) {
        queue.offer(task)
    }

    /**
     * Waits until all currently enqueued and active tasks are completed,
     * then stops the worker thread. Blocks the caller until completion.
     *
     * Should be called during application shutdown to ensure no pending config is lost.
     */
    fun shutdownAndWait() {
        isRunning.set(false)
        synchronized(lock) {
            while (activeCount.get() > 0 || queue.isNotEmpty()) {
                lock.wait()
            }
        }
    }

    /**
     * A single sync/save operation, representing a config object to write back to the database.
     *
     * @property key   the cache/database key of the config object
     * @property value the config object instance to persist
     * @property type  the class type of the config object
     */
    data class SyncTask(
        val key: String,
        val value: Any,
        val type: Class<*>
    )
}
