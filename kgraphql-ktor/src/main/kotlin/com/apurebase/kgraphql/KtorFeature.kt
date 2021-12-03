package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.decodeFromString

class GraphQL(val schema: Schema) {

    class Configuration: SchemaConfigurationDSL() {
        fun schema(block: SchemaBuilder.() -> Unit) {
            schemaBlock = block
        }

        /**
         * This adds support for opening the graphql route within the browser
         */
        var playground: Boolean = false

        var endpoint: String = "/graphql"

        fun context(block: ContextBuilder.(ApplicationCall) -> Unit) {
            contextSetup = block
        }

        fun wrap(block: Route.(next: Route.() -> Unit) -> Unit) {
            wrapWith = block
        }

        internal var contextSetup: (ContextBuilder.(ApplicationCall) -> Unit)? = null
        internal var wrapWith: (Route.(next: Route.() -> Unit) -> Unit)? = null
        internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null

    }


    companion object Feature: ApplicationFeature<Application, Configuration, GraphQL> {
        override val key = AttributeKey<GraphQL>("KGraphQL")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): GraphQL {
            val config = Configuration().apply(configure)
            val schema = KGraphQL.schema {
                configuration = config
                config.schemaBlock?.invoke(this)
            }

            val routing: Routing.() -> Unit = {
                val routing: Route.() -> Unit = {
                    route(config.endpoint) {
                        post {
                            val request = decodeFromString(GraphqlRequest.serializer(), call.receiveText())
                            val ctx = context {
                                config.contextSetup?.invoke(this, call)
                            }
                            val result = schema.execute(request.query, request.variables.toString(), ctx)
                            call.respondText(result, contentType = ContentType.Application.Json)
                        }
                        if (config.playground) get {
                            @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                            val playgroundHtml = KtorGraphQLConfiguration::class.java.classLoader.getResource("playground.html").readBytes()
                            call.respondBytes(playgroundHtml, contentType = ContentType.Text.Html)
                        }
                    }
                }

                config.wrapWith?.invoke(this, routing) ?: routing(this)
            }

            pipeline.featureOrNull(Routing)?.apply(routing) ?: pipeline.install(Routing, routing)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                try {
                    coroutineScope {
                        proceed()
                    }
                } catch (e: Throwable) {
                    if (e is GraphQLError) {
                        context.respond(HttpStatusCode.OK, e.serialize())
                    } else throw e
                }
            }
            return GraphQL(schema)
        }

        /**
         * ima9dan Added extensions to error response json
         * TODO If you're having trouble writing this here, move it elsewhere
         */
        fun Collection<*>.toJsonElement(): JsonElement {
            val list: MutableList<JsonElement> = mutableListOf()
            this.forEach {
                val value = it as? Any ?: return@forEach
                when(value) {
                    is Number -> list.add(JsonPrimitive(value))
                    is Boolean -> list.add(JsonPrimitive(value))
                    is String -> list.add(JsonPrimitive(value))
                    is Map<*, *> -> list.add((value).toJsonElement())
                    is Collection<*> -> list.add(value.toJsonElement())
                    is Array<*> -> list.add(value.toList().toJsonElement())
                    else -> list.add(JsonPrimitive(value.toString())) // other type
                }
            }
            return JsonArray(list)
        }
        /**
         * ima9dan Added extensions to error response json
         * TODO If you're having trouble writing this here, move it elsewhere
         */
        fun Map<*, *>.toJsonElement(): JsonElement {
            val map: MutableMap<String, JsonElement> = mutableMapOf()
            this.forEach {
                val key = it.key as? String ?: return@forEach
                val value = it.value ?: return@forEach
                when(value) {
                    is Number? -> JsonPrimitive(value)
                    is Boolean? -> JsonPrimitive(value)
                    is String? -> JsonPrimitive(value)
                    is Map<*, *> -> map[key] = (value).toJsonElement()
                    is Collection<*> -> map[key] = value.toJsonElement()
                    is Array<*> -> map[key] = value.toList().toJsonElement()
                    else -> map[key] = JsonPrimitive(value.toString())  // other type
                }
            }
            return JsonObject(map)
        }

        private fun GraphQLError.serialize(): String = buildJsonObject {
            put("errors", buildJsonArray {
                addJsonObject {
                    put("message", message)
                    put("locations", buildJsonArray {
                        locations?.forEach {
                            addJsonObject {
                                put("line", it.line)
                                put("column", it.column)
                            }
                        }
                    })
                    put("path", buildJsonArray {
                        // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                    })
                    /**
                     * ima9dan Added extensions to error response json
                     */
                    extensions?.let {
                        put("extensions", buildJsonObject {
                            it.forEach { (key, value) ->
                                when(value) {
                                    is Number? -> put(key, value)
                                    is String? -> put(key, value)
                                    is Boolean? -> put(key, value)
                                    is Map<*,*> -> put(key, value.toJsonElement())
                                    is Collection<*> -> put(key, value.toJsonElement())
                                    is Array<*> -> put(key, value.toList().toJsonElement())
                                    else -> put(key, JsonPrimitive(value.toString()))  // other type
                                }
                            }
                        })
                    }
                }
            })
        }.toString()
    }

}
