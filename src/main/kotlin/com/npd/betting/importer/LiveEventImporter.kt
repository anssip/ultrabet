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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LiveEventImporter(private val eventRepository: EventRepository, private val service: EventService) {
  val logger: Logger = LoggerFactory.getLogger(EventImporter::class.java)

  fun getEventApiURL(sport: String, eventId: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/events/$eventId/odds/?&markets=h2h&bookmakers=bet365,betfair,unibet_eu,betclic&dateFormat=unix&apiKey=${EventImporter.API_KEY}"
  }

  fun getScoresApiURL(sport: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/scores/?daysFrom=2&&markets=${EventImporter.MARKETS}&dateFormat=unix&apiKey=${EventImporter.API_KEY}"
  }

  @Scheduled(fixedDelay = 60, timeUnit = java.util.concurrent.TimeUnit.SECONDS)
  @Transactional
  fun import() {
    runBlocking {
      importLiveEvents()
    }
  }

  suspend fun importLiveEvents() {
    val liveEvents = eventRepository.findByIsLiveTrueAndCompletedFalse()
    logger.debug("Found ${liveEvents.size} live events")

    val eventData = fetchEvents(liveEvents)
    logger.debug("Fetched ${eventData.size} events from bets-api.com")

    val notfound = eventData.mapNotNull { it.second }
    notfound.forEach() {
      service.updateCompleted(it)
    }

    val sports = eventData.mapNotNull { it.first?.sport_key }.distinct()
    logger.debug("Fetching scores for $sports sports")
    sports.forEach() {
      val eventsWithScores = fetchScores(it)
      if (eventsWithScores.isNotEmpty()) {
        eventsWithScores.forEach() { eventWithScores: EventData ->
          val event = liveEvents.find() { liveEvent -> liveEvent.externalId == eventWithScores.id }
          if (event !== null) {
            val eventWithOdds =
              eventData.mapNotNull { it.first }.find() { eventData -> eventData.id == eventWithScores.id }
            eventWithScores.bookmakers = eventWithOdds?.bookmakers
            service.saveEventAndOdds(eventWithScores, true)
            service.saveScores(eventWithScores, event)
          }
        }
      }
    }
  }

  suspend fun fetchEvents(events: List<Event>): List<Pair<EventData?, Int?>> {
    val data = events.map() {
      logger.debug("Fetching odds for event ${it.externalId} and sport ${it.sport.key}")
      val response: HttpResponse =
        httpClient.request(getEventApiURL(it.sport.key, it.externalId!!)) {
          method = HttpMethod.Get
        }
      if (response.status != HttpStatusCode.OK) {
        logger.error("Failed to fetch events: response.status: ${response.status}: ${response.bodyAsText()}")
        //throw IllegalStateException("Failed to fetch events: response.status: ${response.status}: ${response.bodyAsText()}")
        Pair(null, it.id)
      } else {
        val responseBody = response.bodyAsText()
        Pair(Json.decodeFromString<EventData>(responseBody), null)
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