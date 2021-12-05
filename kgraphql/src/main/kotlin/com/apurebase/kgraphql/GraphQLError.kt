package com.apurebase.kgraphql

import com.apurebase.kgraphql.schema.model.ast.ASTNode
import com.apurebase.kgraphql.schema.model.ast.Location.Companion.getLocation
import com.apurebase.kgraphql.schema.model.ast.Source

open class GraphQLError(

    /**
     * A message describing the Error for debugging purposes.
     */
    message: String,

    /**
     * An array of GraphQL AST Nodes corresponding to this error.
     */
    val nodes: List<ASTNode>? = null,

    /**
     * The source GraphQL document for the first location of this error.
     *
     * Note that if this Error represents more than one node, the source may not
     * represent nodes after the first node.
     */
    val source: Source? = null,

    /**
     * An array of character offsets within the source GraphQL document
     * which correspond to this error.
     */
    val positions: List<Int>? = null,

    /**
     * The original error thrown from a field resolver during execution.
     */
    val originalError: Throwable? = null,

    /**
     * ima9dan Added extensions to error response json
     */
    val extensionsErrorCode: String? = "INTERNAL",
    val extensionsErrorDetail: Map<String, Any?>? = null
) : Exception(message) {

    constructor(message: String, node: ASTNode?) : this(message, nodes = node?.let(::listOf))

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
            positions.map { pos -> getLocation(source, pos) }
        } else nodes?.mapNotNull { node ->
            node.loc?.let { getLocation(it.source, it.start) }
        }
    }

    val extensions: Map<String,Any?>?by lazy {
        val extensions = mutableMapOf<String,Any?>()
        extensionsErrorCode?.let{  extensions.put("code",extensionsErrorCode) }
        extensionsErrorDetail?.let { extensions.put("detail",extensionsErrorDetail) }
        extensions
    }

    /**
     * use only debug
     */
    fun debugInfo(): Map<String,Any?> {
        val exception = mutableMapOf<String,Any?>()
        val stackList = this.stackTrace
        if (!stackList[0].fileName.isNullOrEmpty()) {
            exception.put("fileName",stackList[0].fileName)
            exception.put("line",stackList[0].lineNumber.toString())
        }
        if (!stackList[0].methodName.isNullOrEmpty()) {
            exception.put("method",stackList[0].methodName)
        }
        if (!stackList[0].className.isNullOrEmpty()) {
            exception.put("classPath", stackList[0].className)
        }
        exception.put("stackTrace", stackList)
        return  exception
    }

    fun prettyPrint(): String {
        var output = message ?: ""

        if (nodes != null) {
            for (node in nodes) {
                if (node.loc != null) {
                    output += "\n\n" + node.loc!!.printLocation()
                }
            }
        } else if (source != null && locations != null) {
            for (location in locations!!) {
                output += "\n\n" + source.print(location)
            }
        }

        return output
    }
}
