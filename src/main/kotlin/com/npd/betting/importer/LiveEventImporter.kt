package com.npd.betting.importer

import com.npd.betting.repositories.EventRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LiveEventImporter(private val eventRepository: EventRepository, private val service: EventService) {
  fun getEventApiURL(eventIds: List<String>): String {
    val eventIdString = eventIds.joinToString(",")
    return "${EventImporter.API_BASE}/sports/upcoming/odds/?regions=eu&markets=h2h&eventIds=${eventIdString}&apiKey=${EventImporter.API_KEY}"
  }

  fun getScoresApiURL(sport: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/scores/?regions=eu&markets=h2h&apiKey=${EventImporter.API_KEY}"
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

    val eventData = fetchEvents(liveEvents.map { it.externalId!! })
    println("Fetched ${eventData.size} events from bets-api.com")

    eventData.forEach() {
      service.saveEventAndOdds(it, true)
    }
    val sports = eventData.map { it.sport_key }.distinct()
    println("Fetching scores for $sports sports")
    sports.forEach() {
      val eventsWithScores = fetchScores(it)
      if (eventsWithScores.isNotEmpty()) {
        eventsWithScores.forEach() {
          val event = liveEvents.find() { liveEvent -> liveEvent.externalId == it.id }
          if (event !== null) {
            service.saveScores(it, event)
          }
        }
      }
    }
  }

  suspend fun fetchEvents(eventIds: List<String>): List<EventData> {
    val response: HttpResponse =
      httpClient.request(getEventApiURL(eventIds)) {
        method = HttpMethod.Get
      }
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch events")
    }
    val responseBody = response.bodyAsText()
    return Json.decodeFromString(ListSerializer(EventData.serializer()), responseBody)
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