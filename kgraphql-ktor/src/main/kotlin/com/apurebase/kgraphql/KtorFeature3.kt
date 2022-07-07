package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class GraphQL3(val schema: Schema) {

    class Configuration: SchemaConfigurationDSL() {
        fun schema(block: SchemaBuilder.() -> Unit) {
            schemaBlock = block
        }

        /**
         * This adds support for opening the graphql route within the browser
         */
        var playground: Boolean = false

        var endpoint: String = "/graphql"

        /**
         * Change 2: Added debug option to GraphQL Configuration (flag to output exception information to extensions)
         */
        var debug: Boolean = false

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


    companion object Feature: Plugin<Application, Configuration, GraphQL> {
        override val key = AttributeKey<GraphQL>("KGraphQL3")

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
                            val request = Json.decodeFromString(GraphqlRequest.serializer(), call.receiveText())
//                            var receiveText = call.receiveText()

                            // ktorの不具合としてContent-Typeにutf-8が指定されていないと、ISO-8859-1になってしまう問題があったが、
                            // ktor2.0で改善された模様
                            // https://youtrack.jetbrains.com/issue/KTOR-789/Make-default-charset-UTF-8-when-using-receiveText-for-applicationjson-request
//                            if (call.request.headers.get(HttpHeaders.ContentType).toString().indexOf("charset") == -1) {
//                                receiveText = String(receiveText.toByteArray(charset("ISO-8859-1")), charset("UTF-8"))
//                            }
//                            val request = decodeFromString(GraphqlRequest.serializer(), receiveText)
                            val ctx = context {
                                config.contextSetup?.invoke(this, call)
                            }
//                            call.application.environment.log.info("ktor aaaaaaa")
//                            println("ktor aaaaaaa")
                            val result = schema.execute(request.query, request.variables.toString(), ctx)
//                            println("ktor bbbbbbbbb")
//                            call.application.environment.log.info("ktor bbbbbb")
                            call.respondText(result, contentType = ContentType.Application.Json)
//                            call.application.environment.log.info("ktor cccccc")

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

            pipeline.pluginOrNull(Routing)?.apply(routing) ?: pipeline.install(Routing, routing)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
//                try {
                coroutineScope {
                    proceed()
                }
//                } catch (e: Throwable) {
//                    if (e is GraphQLError) {
//                        context.respond(HttpStatusCode.OK, e.serialize(config.debug))
//                    } else throw e
//                }
            }
            return GraphQL(schema)
        }
    }

}
