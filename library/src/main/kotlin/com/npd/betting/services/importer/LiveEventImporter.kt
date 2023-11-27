package com.npd.betting.services.importer

import com.npd.betting.Props
import com.npd.betting.model.Event
import com.npd.betting.repositories.EventRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class LiveEventImporter(
  private val props: Props,
  private val eventRepository: EventRepository,
  private val service: EventService
) {
  val logger: Logger = LoggerFactory.getLogger(EventImporter::class.java)

  fun getEventApiURL(sport: String, eventId: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/events/$eventId/odds/?&markets=h2h&bookmakers=bet365,betfair,unibet_eu,betclic&dateFormat=unix&apiKey=${props.getOddsApiKey()}"
  }

  @Scheduled(fixedDelay = 60, timeUnit = java.util.concurrent.TimeUnit.SECONDS)
  @Transactional
  open fun import() {
    runBlocking {
      importLiveEvents()
    }
  }

  suspend fun importLiveEvents() {
    val liveEvents = withContext(Dispatchers.IO) {
      eventRepository.findByIsLiveTrueAndCompletedFalse()
    }
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
      val eventsWithScores = service.fetchScores(it)
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

}