package com.npd.betting.importer

import com.npd.betting.model.Event
import com.npd.betting.repositories.EventRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LiveEventImporter(private val eventRepository: EventRepository, private val service: EventService) {
  fun getEventApiURL(sport: String, eventId: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/events/$eventId/odds/?&markets=h2h&regions=uk,us,us2,eu,au&apiKey=${EventImporter.API_KEY}"
  }

  fun getScoresApiURL(sport: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/scores/?daysFrom=2&&markets=h2h&apiKey=${EventImporter.API_KEY}"
  }

  @Scheduled(fixedDelay = 10000)
  @Transactional
  fun import() {
    runBlocking {
      importLiveEvents()
    }
  }

  suspend fun importLiveEvents() {
    val liveEvents = eventRepository.findByIsLiveTrueAndCompletedFalse()
    println("Found ${liveEvents.size} live events")

    val eventData = fetchEvents(liveEvents)
    println("Fetched ${eventData.size} events from bets-api.com")

    val sports = eventData.map { it.sport_key }.distinct()
    println("Fetching scores for $sports sports")
    sports.forEach() {
      val eventsWithScores = fetchScores(it)
      if (eventsWithScores.isNotEmpty()) {
        eventsWithScores.forEach() { eventWithScores: EventData ->
          val event = liveEvents.find() { liveEvent -> liveEvent.externalId == eventWithScores.id }
          if (event !== null) {
            val eventWithOdds = eventData.find() { eventData -> eventData.id == eventWithScores.id }
            eventWithScores.bookmakers = eventWithOdds?.bookmakers
            service.saveEventAndOdds(eventWithScores, true)
            service.saveScores(eventWithScores, event)
          }
        }
      }
    }
  }

  suspend fun fetchEvents(events: List<Event>): List<EventData> {
    val data = events.mapNotNull() {
      println("Fetching odds for event ${it.externalId} and sport ${it.sport.key}")
      val response: HttpResponse =
        httpClient.request(getEventApiURL(it.sport.key, it.externalId!!)) {
          method = HttpMethod.Get
        }
      if (response.status != HttpStatusCode.OK) {
        println("Failed to fetch events: response.status: ${response.status}: ${response.bodyAsText()}")
        //throw IllegalStateException("Failed to fetch events: response.status: ${response.status}: ${response.bodyAsText()}")
        // TODO: maybe update as completed
        null
      } else {
        val responseBody = response.bodyAsText()
        Json.decodeFromString<EventData>(responseBody)
      }
    }
    return data
  }

  suspend fun fetchScores(sport: String): List<EventData> {
    val response: HttpResponse =
      httpClient.request(getScoresApiURL(sport)) {
        method = HttpMethod.Get
      }
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch scores: ${response.status}: ${response.bodyAsText()}")
    }
    val responseBody = response.bodyAsText()
    return Json.decodeFromString(ListSerializer(EventData.serializer()), responseBody)
  }
}