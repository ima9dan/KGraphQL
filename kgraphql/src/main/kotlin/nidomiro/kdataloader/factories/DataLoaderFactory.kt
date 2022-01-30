package nidomiro.kdataloader.factories

import com.apurebase.kgraphql.Context
import nidomiro.kdataloader.*

typealias DataLoaderFactoryMethod<K, R> = (options: DataLoaderOptions<K, R>, batchLoader: BatchLoader<K, R>) -> DataLoader<K, R>

open class DataLoaderFactory<K, R>(
    @Suppress("MemberVisibilityCanBePrivate")
    protected val optionsFactory: () -> DataLoaderOptions<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val batchLoader: BatchLoader<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val cachePrimes: Map<K, ExecutionResult<R>>,
    protected val factoryMethod: DataLoaderFactoryMethod<K, R>
) {

    suspend fun constructNew(ctx:Context): DataLoader<K, R> {
        val dataLoader = factoryMethod(optionsFactory(), batchLoader)
        dataLoader.ctx = ctx
        cachePrimes.forEach { (key, value) -> dataLoader.prime(key, value) }
        return dataLoader
    }
}
