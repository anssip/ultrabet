package com.npd.betting.graphql

import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.IOException


@Configuration
class GraphQLConfig(
  val eventDataFetcher: EventDataFetcher,
  val liveEventDataFetcher: LiveEventDataFetcher,
  val eventScoreUpdatesDataFetcher: EventScoreUpdatesDataFetcher
) {

  @Value("classpath:graphql/common.graphqls")
  var commonSchemaResource: Resource? = null

  @Value("classpath:graphql/private.graphqls")
  var privateSchemaResource: Resource? = null

  @Bean("publicGraphQL")
  @Throws(IOException::class)
  fun graphQL(): GraphQL {
    val typeRegistry = SchemaParser()
      .parse(commonSchemaResource!!.file)

    val runtimeWiring = buildRuntimeWiring(false)
    val schema = SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
    return GraphQL.newGraphQL(schema).build()
  }

  @Bean("privateGraphQL")
  @Throws(IOException::class)
  fun privateGraphQL(): GraphQL {
    val commonSchemaContent = getResourceAsString(commonSchemaResource)
    val privateSchemaContent = getResourceAsString(privateSchemaResource)

    val typeRegistry = SchemaParser()
      .parse(commonSchemaContent + privateSchemaContent) // Combine the schema strings

    val runtimeWiring = buildRuntimeWiring(true)
    val schema = SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring)
    return GraphQL.newGraphQL(schema).build()
  }

  fun buildRuntimeWiring(private: Boolean): RuntimeWiring {
    val builder = RuntimeWiring.newRuntimeWiring()
      .type("Query") { builder ->
        builder.dataFetcher("listEvents", eventDataFetcher)
        builder.dataFetcher("listLiveEvents", liveEventDataFetcher)
      }
      .type("Subscription") { builder ->
        builder.dataFetcher("eventScoresUpdated", eventScoreUpdatesDataFetcher)
      }
//    if (private) {
//      builder.type("Mutation") { builder ->
//        builder.dataFetcher("createEvent", eventDataFetcher)
//      }
//    }
    return builder.build()
  }

  @Throws(IOException::class)
  private fun getResourceAsString(resource: Resource?): String {
    if (resource == null) {
      throw IOException("Resource not found")
    }
    return String(resource.inputStream.readBytes())
  }
}