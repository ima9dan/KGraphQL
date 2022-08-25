package com.apurebase.kgraphql.schema.dsl.types

import com.apurebase.kgraphql.defaultKQLTypeName
import com.apurebase.kgraphql.schema.dsl.ItemDSL
import com.apurebase.kgraphql.schema.dsl.PropertyDSL
import com.apurebase.kgraphql.schema.model.PropertyDef
import com.apurebase.kgraphql.schema.model.TypeDef
import kotlin.reflect.KClass


class InputTypeDSL<T : Any>(val kClass: KClass<T>) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    internal val extensionProperties = mutableSetOf<PropertyDef.Function<T, *>>()


    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val dsl = PropertyDSL(name, block)
        extensionProperties.add(dsl.toKQLProperty())
    }
//
//    internal fun toKQLObject() : TypeDef.Object<T> {
//        return TypeDef.Object(
//            name = name,
//            kClass = kClass,
//            kotlinProperties = describedKotlinProperties.toMap(),
//            extensionProperties = extensionProperties.toList(),
//            dataloadExtensionProperties = dataloadedExtensionProperties.toList(),
//            unionProperties = unionProperties.toList(),
//            transformations = transformationProperties.associateBy { it.kProperty },
//            description = description
//        )
//    }
}
