package com.npd.betting.services.importer

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Serializable
data class EventData(
  val id: String,
  val sport_key: String,
  val sport_title: String,
  val commence_time: Long,
  val home_team: String,
  val away_team: String,
  var bookmakers: List<Bookmaker>? = null,
  var completed: Boolean? = false,
  var scores: List<Score>? = null,
  var last_update: Long? = null
) {
  fun isLive(): Boolean {
    return Date().time > commence_time * 1000
  }
}

@Serializable
data class SportData(
  val key: String,
  val active: Boolean,
  val group: String,
  val description: String,
  val title: String,
  val has_outrights: Boolean
)

@Serializable
data class Score(
  val name: String,
  val score: String
)

@Serializable
data class Bookmaker(
  val key: String,
  val title: String,
  val last_update: Long? = null,
  val markets: List<MarketData>
)

@Serializable
data class MarketData(
  val key: String,
  val last_update: Long,
  val outcomes: List<MarketOptionData>
)

@Serializable
data class MarketOptionData(
  val name: String,
  val price: Double
)


@Component
open class EventImporter(private val service: EventService) {
  val logger: Logger = LoggerFactory.getLogger(EventImporter::class.java)

  companion object {
    const val API_KEY = "e0f4fe902a9673daf7a78104feb2523e"
    const val API_BASE = "https://api.the-odds-api.com/v4/"
    const val MARKETS = "h2h"
    private const val BOOKMAKERS = "bet365,betfair,unibet_eu,betclic"

    const val EVENTS_URL =
      "$API_BASE/sports/upcoming/odds/?markets=$MARKETS&bookmakers=$BOOKMAKERS&dateFormat=unix&apiKey=$API_KEY"
    const val SPORTS_URL = "$API_BASE/sports/?apiKey=$API_KEY"
  }

  @Scheduled(fixedRate = 60, timeUnit = java.util.concurrent.TimeUnit.MINUTES) // Poll the API every 60 minutes
  @Transactional
  open fun importEvents() {
    runBlocking {
      doImport()
    }
  }

  suspend fun doImport() {
    val sports = this.fetchSports()
    logger.debug("Importing ${sports.size} sports")
    service.importSports(sports)

    val events = this.fetchEvents()
    logger.debug("Importing ${events.size} events")
    service.importEvents(events)
  }

  suspend fun fetchSports(): List<SportData> {
    val response: HttpResponse = httpClient.get(SPORTS_URL)
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch sports")
    }
    val responseBody = response.bodyAsText()
    return Json.decodeFromString(ListSerializer(SportData.serializer()), responseBody)
  }

  suspend fun fetchEvents(): List<EventData> {
    val response: HttpResponse = httpClient.get(EVENTS_URL)
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch events")
    }
    val responseBody = response.bodyAsText()
    return Json.decodeFromString(ListSerializer(EventData.serializer()), responseBody)
  }

}
