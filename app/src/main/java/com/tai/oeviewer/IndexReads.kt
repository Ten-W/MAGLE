package com.tai.oeviewer

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/** Yield between reads without leaving thousands of queued requests ahead of an edit. */
internal fun <T> readIndexChunk(workers: ExecutorService, ids: List<String>, paused: () -> Boolean,
    fetch: (String) -> T, parallelism: Int = 8, accept: (String, Result<T>) -> Unit): Boolean {
    require(ids.size <= 8)
    require(parallelism in 1..8)
    if (paused()) return false
    val completion = ExecutorCompletionService<Pair<String, Result<T>>>(workers)
    val futures = mutableListOf<java.util.concurrent.Future<Pair<String, Result<T>>>>()
    var submitted = 0
    fun submitNext() {
        val id = ids[submitted++]
        futures += completion.submit(Callable { id to runCatching { fetch(id) } })
    }
    repeat(minOf(parallelism, ids.size)) { submitNext() }
    try {
        var received = 0
        while (received < ids.size) {
            if (paused()) return false
            val finished = completion.poll(250, TimeUnit.MILLISECONDS) ?: continue
            val (id, result) = finished.get()
            accept(id, result)
            received++
            if (submitted < ids.size && !paused()) submitNext()
        }
        return true
    } finally { futures.filterNot { it.isDone }.forEach { it.cancel(true) } }
}
