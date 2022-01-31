package nidomiro.kdataloader

import com.apurebase.kgraphql.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import nidomiro.kdataloader.statistics.SimpleStatisticsCollector

class TimedAutoDispatcherImpl<K, R>(
    options: TimedAutoDispatcherDataLoaderOptions<K, R>,
    batchLoader: BatchLoader<K, R>,
    parent: Job? = null,
) : SimpleDataLoaderImpl<K, R>(options, SimpleStatisticsCollector(), batchLoader), CoroutineScope {

    override var ctx: Context = Context(emptyMap())
    private val autoChannel = Channel<Unit>()
    override val coroutineContext = Job(parent)

    init {
        launch {
            var job: Job? = null
            for (msg in autoChannel) {
                job?.cancel()
                job = launch {
                    delay(options.waitInterval)
                    if (isActive) {
                        dispatch(ctx)
                    }
                }
            }
        }
    }
//    init {
//        launch {
//            var job: Job? = null
//            var safeFlg:Boolean = false
//            for (msg in autoChannel) {
//                if (!safeFlg) {
//                    job?.cancel()
//                }
//                job = launch {
//                    delay(options.waitInterval)
//                    if (isActive) {
//                        safeFlg = true
//                        dispatch(ctx)
//                        safeFlg = false
//                    }
//                }
//            }
//        }
//    }


//    init {
////    println("開始")
//        launch {
//            var job: Job? = null
//            for (msg in autoChannel) {
//                val active = isActive
//                if (job !== null) {
//                    if (!job.isCompleted) {
//
//                    } else {
//                        job?.cancel()
//                        job = launch {
//                            delay(options.waitInterval)
//                            if (active) {
//                                dispatch(ctx)
//                            }
//                        }
//                    }
//                } else {
//                    job?.cancel()
//                    job = launch {
//                        delay(options.waitInterval)
//                        if (active) {
//                            dispatch(ctx)
//                        }
//                    }
//                }
//            }
//            dispatch(ctx)
//        }
////    println("終了")
//    }

    suspend fun cancel() {
        coroutineContext.cancel()
        autoChannel.close()
        dispatch(ctx)
    }

    override suspend fun loadAsync(key: K): Deferred<R> {
        return super.loadAsync(key).also { autoChannel.send(Unit) }
    }

    override suspend fun loadManyAsync(vararg keys: K): Deferred<List<R>> {
        return super.loadManyAsync(*keys).also { autoChannel.send(Unit) }
    }

    override suspend fun clear(key: K) {
        super.clear(key).also { autoChannel.send(Unit) }
    }

    override suspend fun clearAll() {
        super.clearAll().also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: R) {
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: Throwable){
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

}
