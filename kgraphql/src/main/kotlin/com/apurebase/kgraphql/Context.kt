package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.introspection.NotIntrospected
import kotlin.reflect.KClass

@NotIntrospected
class Context(private val map: Map<Any, Any>) {

    private var flexibleMap:MutableMap<Any, Any>
    init {
        flexibleMap = mutableMapOf()
    }

    operator fun <T : Any> get(kClass: KClass<T>): T? {
        val value = map[kClass]
        @Suppress("UNCHECKED_CAST")
        return  if(kClass.isInstance(value)) value as T else null
    }

    inline fun <reified T : Any> get() : T? = get(T::class)

    fun <T : Any>getValue(key: String): T? {
        return if (flexibleMap.containsKey(key)) flexibleMap[key] as T else null
    }
    fun setValue(key: String, value:Any) {
        flexibleMap[key] = value
    }
}