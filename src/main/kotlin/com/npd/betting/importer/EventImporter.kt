package com.npd.betting.importer

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Serializable
data class EventData(
  val id: String,
  val sport_key: String,
  val sport_title: String,
  @Serializable(with = DateSerializer::class)
  val commence_time: Date,
  val home_team: String,
  val away_team: String,
  var bookmakers: List<Bookmaker>? = null,
  var completed: Boolean? = false,
  var scores: List<Score>? = null,
  @Serializable(with = DateSerializer::class)
  var last_update: Date? = null
) {
  fun isLive(): Boolean {
    return Date().after(commence_time) && !completed!!
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
  @Serializable(with = DateSerializer::class)
  val last_update: Date? = null,
  val markets: List<MarketData>
)

@Serializable
data class MarketData(
  val key: String,
  @Serializable(with = DateSerializer::class)
  val last_update: Date,
  val outcomes: List<MarketOptionData>
)

@Serializable
data class MarketOptionData(
  val name: String,
  val price: Double
)


@Component
class EventImporter(private val service: EventService) {
  companion object {
    const val API_KEY = "fb2a903bfc8c151b2bd88f3ebd16dd99"
    const val API_BASE = "https://api.the-odds-api.com/v4/"
    const val EVENTS_URL =
      "$API_BASE/sports/upcoming/odds/?regions=eu&markets=h2h&apiKey=$API_KEY"
    const val SPORTS_URL = "$API_BASE/sports/?apiKey=$API_KEY"
  }

  @Scheduled(fixedRate = 60000) // Poll the API every minute (60000 milliseconds)
  @Transactional
  fun importEvents() {
    runBlocking {
      doImport()
    }
  }

  suspend fun doImport() {
    val sports = this.fetchSports()
    println("Importing ${sports.size} sports")
    service.importSports(sports)

    val events = this.fetchEvents()
    println("Importing ${events.size} events")
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
