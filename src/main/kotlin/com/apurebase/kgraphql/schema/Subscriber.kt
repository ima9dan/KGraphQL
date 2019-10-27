package com.apurebase.kgraphql.schema

import com.fasterxml.jackson.databind.ObjectWriter
import java.util.concurrent.Flow

interface Subscriber {
    fun onSubscribe(subscription: Flow.Subscription)

    fun onNext(item: Any?)

    fun setArgs(args: Array<String>)

    fun onError(throwable: Throwable)

    fun onComplete()

    fun setObjectWriter(objectWriter: ObjectWriter)
}