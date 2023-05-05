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

@Component
class LiveEventImporter(private val eventRepository: EventRepository, private val service: EventService) {
  fun getEventApiURL(eventIds: List<String>): String {
    val eventIdString = eventIds.joinToString(",")
    return "https://api.the-odds-api.com/v4/sports/upcoming/odds/?regions=eu&markets=h2h&eventIds=${eventIdString}&apiKey=${EventImporter.API_KEY}"
  }

  @Scheduled(fixedDelay = 1000)
  fun import() {
    runBlocking {
      importLiveEvents()
    }
  }

  suspend fun importLiveEvents() {
    val liveEvents = eventRepository.findByIsLiveTrueAndCompletedFalse()
    val eventData = fetchEvents(liveEvents.map { it.externalId!! })

    eventData.forEach() {
      service.saveEvent(it)
      // TODO: import also scores
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
}