package com.github.pgutkowski.kgraphql.schema.impl

import com.github.pgutkowski.kgraphql.request.Request
import com.github.pgutkowski.kgraphql.request.RequestParser
import com.github.pgutkowski.kgraphql.request.Variables
import com.github.pgutkowski.kgraphql.schema.Schema
import com.github.pgutkowski.kgraphql.schema.model.KQLMutation
import com.github.pgutkowski.kgraphql.schema.model.KQLQuery
import com.github.pgutkowski.kgraphql.schema.model.KQLType

class DefaultSchema(
        val queries: List<KQLQuery<*>>,
        val mutations: List<KQLMutation<*>>,
        val objects: List<KQLType.Object<*>>,
        val scalars: List<KQLType.Scalar<*>>,
        val enums: List<KQLType.Enumeration<*>>,
        val unions: List<KQLType.Union>
) : Schema {

    val structure = SchemaStructure.of(this)

    val requestExecutor = RequestExecutor(this)

    /**
     * objects for request handling
     */
    private val requestParser = RequestParser { resolveActionType(it) }

    override fun handleRequest(request: String, variables: String?): String {
        try {
            val parsedVariables = variables?.let { Variables(variables) } ?: Variables()
            val executionPlan = structure.createExecutionPlan(requestParser.parse(request))
            return requestExecutor.execute(executionPlan, parsedVariables)
        } catch(e: Exception) {
            return "{\"errors\" : { \"message\": \"Caught ${e.javaClass.canonicalName}: ${e.message?.replace("\"", "\\\"")}\"}}"
        }
    }

    fun resolveActionType(token: String): Request.Action {
        if (queries.any { it.name.equals(token, true) }) return Request.Action.QUERY
        if (mutations.any { it.name.equals(token, true) }) return Request.Action.MUTATION
        throw IllegalArgumentException("Cannot infer request type for name $token")
    }
}