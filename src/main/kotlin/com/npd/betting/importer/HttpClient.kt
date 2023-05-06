package com.npd.betting.importer

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


var httpClient = HttpClient(CIO) {
  install(ContentNegotiation) {
    json(Json {
      ignoreUnknownKeys = true
      isLenient = true
    })
  }
  install(Logging) {
    logger = Logger.DEFAULT
    level = LogLevel.NONE
  }
}
