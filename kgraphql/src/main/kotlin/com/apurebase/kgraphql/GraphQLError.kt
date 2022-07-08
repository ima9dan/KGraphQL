package com.apurebase.kgraphql

import com.apurebase.kgraphql.helpers.toJsonElement
import com.apurebase.kgraphql.schema.model.ast.ASTNode
import com.apurebase.kgraphql.schema.model.ast.Location.Companion.getLocation
import com.apurebase.kgraphql.schema.model.ast.Source
import kotlinx.serialization.json.*

open class GraphQLError(

    /**
     * A message describing the Error for debugging purposes.
     */
    message: String,

    /**
     * An array of GraphQL AST Nodes corresponding to this error.
     */
    var nodes: List<ASTNode>? = null,

    /**
     * The source GraphQL document for the first location of this error.
     *
     * Note that if this Error represents more than one node, the source may not
     * represent nodes after the first node.
     */
    var source: Source? = null,

    /**
     * An array of character offsets within the source GraphQL document
     * which correspond to this error.
     */
    var positions: List<Int>? = null,

    /**
     * The original error thrown from a field resolver during execution.
     */
    originalError: Throwable? = null,

    /**
     * Change 1: Added extensions to the error response.
     */
    var status: Int? = 500,
    var code: String? = "INTERNAL_SERVER_ERROR",
    var detail: Map<String, Any?>? = null
) : Exception(getMessage(message,originalError)) {

    constructor(message: String, node: ASTNode?) : this(message, nodes = node?.let(::listOf))
    constructor(status: Int = 500,code: String?, message: String,  detail:Map<String, Any?>?) : this(message,null,null,null,null,status,code,detail )
    constructor(status: Int = 500,code: String?, message: String) : this(message,null,null,null,null,status,code )
    constructor(status: Int = 500,code: String?, message: String,  detail:Map<String, Any?>?, originalError: Throwable? = null) : this(message,null,null,null,originalError,status,code,detail )
    constructor(status: Int = 500,code: String?, message: String, originalError: Throwable? = null) : this(message,null,null,null,originalError,status,code ,null)

    val originalError:Throwable?
    init {
        this.originalError = rootCause(originalError)
        if (this.originalError is GraphQLError) {
            this.status = this.originalError.status
            this.code = this.originalError.code
            this.detail = this.originalError.detail
            this.positions = this.originalError.positions
            this.source = this.originalError.source
            this.nodes = this.originalError.nodes
        }
    }

    companion object {
        fun rootCause(throwable: Throwable?):Throwable? {
            if (throwable == null) {
                return null
            } else {
                var rootCause:Throwable = throwable
                while (rootCause.cause != null && rootCause.cause !== rootCause) {
                    rootCause = rootCause.cause!!
                }
                return rootCause
            }
        }
        fun getMessage(message: String,throwable: Throwable? ):String {
            val myError = rootCause(throwable)
            if (myError != null) {
                if (myError.message.isNullOrEmpty()) {
                    return myError.toString()
                } else {
                    return myError.message!!
                }
            } else {
                return message
            }
        }
    }


    /**
     * An array of { line, column } locations within the source GraphQL document
     * that correspond to this error.
     *
     * Errors during validation often contain multiple locations, for example to
     * point out two things with the same name. Errors during execution include a
     * single location, the field that produced the error.
     */
    val locations: List<Source.LocationSource>? by lazy {
            if (positions != null && source != null) {
            positions!!.map { pos -> getLocation(source!!, pos) }
        } else nodes?.mapNotNull { node ->
            node.loc?.let { getLocation(it.source, it.start) }
        }
    }

    fun prettyPrint(): String {
        var output = message ?: ""

        if (nodes != null) {
            for (node in nodes!!) {
                if (node.loc != null) {
                    output += "\n\n" + node.loc!!.printLocation()
                }
            }
        } else if (source != null && locations != null) {
            for (location in locations!!) {
                output += "\n\n" + source!!.print(location)
            }
        }

        return output
    }

    /**
     * Change 1: Added extensions to the error response.
     * Change 2: Added debug option to GraphQL Configuration (flag to output exception information to extensions)
     * Change 3: Moved serialize (), which was defined as an extension of GraphQLError, into GraphQLError. option
     */
    open val extensions: Map<String,Any?>?by lazy {
        val extensions = mutableMapOf<String,Any?>()
        status?.let{  extensions.put("status", status) }
        code?.let{  extensions.put("code", code) }
        detail?.let { extensions.put("detail",detail) }
        extensions
    }

    open fun extensionsDebug(): Map<String,Any?> {
        val extensions = mutableMapOf<String,Any?>()
        status?.let{  extensions.put("status", status) }
        code?.let{  extensions.put("code", code) }
        detail?.let { extensions.put("detail",detail) }
        extensions.put("debug",this.debugInfo())
        return extensions
    }

    open fun debugInfo(): Map<String,Any?> {
        val exception = mutableMapOf<String,Any?>()
        val stackListTmp: Array<StackTraceElement>

        if (this.originalError != null) {
            stackListTmp = this.originalError.stackTrace
        } else {
            stackListTmp = this.stackTrace
        }

        if (!stackListTmp[0].fileName.isNullOrEmpty()) {
            exception.put("fileName",stackListTmp[0].fileName)
            exception.put("line",stackListTmp[0].lineNumber.toString())
        }
        if (!stackListTmp[0].methodName.isNullOrEmpty()) {
            exception.put("method",stackListTmp[0].methodName)
        }
        if (!stackListTmp[0].className.isNullOrEmpty()) {
            exception.put("classPath", stackListTmp[0].className)
        }
        exception.put("stackTrace", stackListTmp)
        return  exception
    }

//    data class DebugInfo(val fileName:String,
//        val line:String,
//                  val method:String,
//                     val classPath:String,
//
//                         )
//    open fun debugInfoByClass(): Map<String,Any?> {
//        val exception = mutableMapOf<String,Any?>()
//        val stackListTmp: Array<StackTraceElement>
//
//        if (this.originalError != null) {
//            stackListTmp = this.originalError.stackTrace
//        } else {
//            stackListTmp = this.stackTrace
//        }
//
//        if (!stackListTmp[0].fileName.isNullOrEmpty()) {
//            exception.put("fileName",stackListTmp[0].fileName)
//            exception.put("line",stackListTmp[0].lineNumber.toString())
//        }
//        if (!stackListTmp[0].methodName.isNullOrEmpty()) {
//            exception.put("method",stackListTmp[0].methodName)
//        }
//        if (!stackListTmp[0].className.isNullOrEmpty()) {
//            exception.put("classPath", stackListTmp[0].className)
//        }
//        exception.put("stackTrace", stackListTmp)
//        return  exception
//    }

    open fun serialize(debug:Boolean=false): String = buildJsonObject {
        put("errors", buildJsonArray {
            addJsonObject {
                put("message", message)
                put("locations", buildJsonArray {
                    locations?.forEach {
                        addJsonObject {
                            put("liane", it.line)
                            put("column", it.column)
                        }
                    }
                })
                put("path", buildJsonArray {
                    // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                })
                if (!debug) {
                    extensions?.let {
                        put("extensions", it.toJsonElement())
                    }
                } else {
                    extensionsDebug().let {
                        put("extensions", it.toJsonElement())
                    }
                }
            }
        })
    }.toString()

    open fun serializeJsonElement(debug:Boolean=false): JsonElement = buildJsonObject {
        put("errors", buildJsonArray {
            addJsonObject {
                put("message", message)
                put("locations", buildJsonArray {
                    locations?.forEach {
                        addJsonObject {
                            put("liane", it.line)
                            put("column", it.column)
                        }
                    }
                })
                put("path", buildJsonArray {
                    // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                })
                if (!debug) {
                    extensions?.let {
                        put("extensions", it.toJsonElement())
                    }
                } else {
                    extensionsDebug().let {
                        put("extensions", it.toJsonElement())
                    }
                }
            }
        })
    }
}
