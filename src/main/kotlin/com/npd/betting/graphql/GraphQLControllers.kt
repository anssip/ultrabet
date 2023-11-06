package com.npd.betting.graphql

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.ExecutionInput
import graphql.ExecutionResult
import graphql.GraphQL
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer


@RestController
class PublicGraphQLController {
  @Autowired
  private lateinit var publicGraphQL: GraphQL

  //  @PostMapping("/graphql")
//  fun graphql(@RequestBody request: String): ResponseEntity<MutableMap<String, Any>> {
//    val result = publicGraphQL.execute(request)
//    return ResponseEntity(result.toSpecification(), HttpStatus.OK)
//  }

  @PostMapping("/graphql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  fun execute(@RequestBody request: String , raw: HttpServletRequest): Map<String, Any> {
    // convert request string to JSON Map
    val objectMapper = ObjectMapper()
    val graphQLRequest = objectMapper.readValue(request, Map::class.java)

    val executionResult: ExecutionResult = publicGraphQL.execute(
      ExecutionInput.newExecutionInput()
        .query(graphQLRequest["query"].toString())
        .operationName(graphQLRequest["operationName"].toString())
        .context(raw)
        .build()
    )
    return executionResult.toSpecification()
  }
}

@RestController
class PrivateGraphQLController {
  @Autowired
  private lateinit var privateGraphQL: GraphQL

  @PostMapping("/private/graphql", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
  @ResponseBody
  fun graphql(@RequestBody request: String): ResponseEntity<MutableMap<String, Any>> {
    val result = privateGraphQL.execute(request)
    return ResponseEntity(result.toSpecification(), HttpStatus.OK)
  }
}
