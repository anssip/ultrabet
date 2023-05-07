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
data class Score(
  val name: String,
  val score: String
)

@Serializable
data class Bookmaker(
  val key: String,
  val title: String,
  @Serializable(with = DateSerializer::class)
  val last_update: Date,
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
    const val API_KEY = "407cf42047dd81f1c87c56d4df701971"
    const val API_BASE = "https://api.the-odds-api.com/v4/"
    const val API_URL =
      "$API_BASE/sports/upcoming/odds/?regions=eu&markets=h2h&apiKey=$API_KEY"
  }

  @Scheduled(fixedRate = 60000) // Poll the API every minute (60000 milliseconds)
  @Transactional
  fun importEvents() {
    runBlocking {
      doImport()
    }
  }

  suspend fun doImport() {
    val events = this.fetchEvents()
    println("Importing ${events.size} events")
    service.importEvents(events)
  }

  suspend fun fetchEvents(): List<EventData> {
    val response: HttpResponse = httpClient.get(API_URL)
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch events")
    }
    val responseBody = response.bodyAsText()
    return Json.decodeFromString(ListSerializer(EventData.serializer()), responseBody)
  }

}
