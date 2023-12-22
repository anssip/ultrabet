package com.npd.betting.services

import com.npd.betting.Props
import com.npd.betting.controllers.AccumulatingSink
import com.npd.betting.model.*
import com.npd.betting.repositories.*
import com.npd.betting.services.importer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class EventService(
  private val props: Props,
  private val betService: BetService,
  private val eventRepository: EventRepository,
  private val marketRepository: MarketRepository,
  private val marketOptionRepository: MarketOptionRepository,
  private val scoreUpdateRepository: ScoreUpdateRepository,
  private val sportRepository: SportRepository,
  private val marketOptionSink: AccumulatingSink<MarketOption>,
  private val scoreUpdatesSink: AccumulatingSink<Event>,
  private val eventStatusUpdatesSink: AccumulatingSink<Event>,
) {
  val logger: Logger = LoggerFactory.getLogger(EventService::class.java)

  fun getScoresApiURL(sport: String): String {
    return "${EventImporter.API_BASE}/sports/$sport/scores/?daysFrom=2&&markets=${EventImporter.MARKETS}&dateFormat=unix&apiKey=${props.getOddsApiKey()}"
  }

  suspend fun importEvents(eventsData: List<EventData>) {
    eventsData.forEach { eventData ->
      val existing = eventRepository.findByExternalId(eventData.id)
      if (existing != null) {
        logger.debug(
          "event commence time: {}, current time: {}, is live based on time? {}",
          Date(eventData.commence_time * 1000),
          Date(),
          eventData.isLive()
        )
        logger.debug("eventData.completed: ${eventData.completed}")

        if (existing.isLive != eventData.isLive() || existing.completed != eventData.completed) {
          existing.isLive = eventData.isLive()
          existing.completed = eventData.completed ?: existing.completed
          if (existing.completed == true) {
            updateScores(existing)
            withContext(Dispatchers.IO) {
              updateEventResult(existing)
            }
          }
          val saved = eventRepository.save(existing)
          logger.info("Event ${saved.id} is now ${if (saved.isLive) "live" else "not live"}. Emitting...")
          eventStatusUpdatesSink.emit(saved)
        } else {
          logger.debug("Event ${eventData.id} already exists, skipping...")
        }
      } else {
        saveEventAndOdds(eventData)
      }
    }
  }

  suspend fun saveEventAndOdds(
    eventData: EventData,
    update: Boolean = false
  ) {

    val event = if (update) {
      val existing = eventRepository.findByExternalId(eventData.id)

        ?: throw Exception("Event ${eventData.id} does not exist")
      logger.info("Updating event ${eventData.id}: ${eventData.home_team} vs ${eventData.away_team}")
      existing.isLive = eventData.isLive()
      existing.completed = eventData.completed ?: existing.completed
      existing.homeTeamName = eventData.home_team
      existing.awayTeamName = eventData.away_team

      if (existing.completed == true) {
        updateScores(existing)
        withContext(Dispatchers.IO) {
          updateEventResult(existing)
        }
      }
      eventRepository.save(existing)
    } else {
      val sportEntity = withContext(Dispatchers.IO) {
        sportRepository.findByKey(eventData.sport_key)
      }
        ?: throw Exception("Sport with key ${eventData.sport_key} does not exist")

      logger.info("Creating event ${eventData.id}: ${eventData.home_team} vs ${eventData.away_team}")
      val newEvent = Event(
        isLive = eventData.isLive(),
        name = "${eventData.home_team} vs ${eventData.away_team}",
        startTime = Timestamp(eventData.commence_time * 1000),
        sport = sportEntity,
        externalId = eventData.id,
        homeTeamName = eventData.home_team,
        awayTeamName = eventData.away_team,
      )
      if (eventData.completed != null) {
        newEvent.completed = eventData.completed ?: newEvent.completed
      }
      if (newEvent.completed!!) {
        updateScores(newEvent)
        withContext(Dispatchers.IO) {
          updateEventResult(newEvent)
        }
      }
      withContext(Dispatchers.IO) {
        eventRepository.save(
          newEvent
        )
      }
    }

    // save bookmakers --> odds
    if (eventData.bookmakers != null) {
      eventData.bookmakers!!.forEach { bookmaker ->
        bookmaker.markets.forEach { marketData ->
          val existingMarket = marketRepository.findByEventIdAndSourceAndName(event.id, bookmaker.key, marketData.key)
          if (existingMarket != null) {
            if (existingMarket.lastUpdated!!.time < marketData.last_update * 1000) {
              // update market
              logger.info("Event ${event.id}, market ${marketData.key}, source: ${bookmaker.key} has been updated")
              updateMarket(event, existingMarket, marketData)
            } else {
              logger.debug("Market did not change")
            }
          } else {
            createMarket(event, marketData, bookmaker)
          }
        }
      }
    }
  }

  private fun updateMarket(
    event: Event,
    existingMarket: Market,
    marketData: MarketData,
  ) {
    existingMarket.isLive = event.isLive
    existingMarket.lastUpdated = Timestamp(marketData.last_update * 1000)

    marketData.outcomes.forEach { marketOptionData ->
      val existingMarketOption = existingMarket.options.find { it.name == marketOptionData.name }

      if (existingMarketOption != null && existingMarketOption.odds != BigDecimal(marketOptionData.price)) {
        existingMarketOption.odds = BigDecimal(marketOptionData.price)
        existingMarketOption.lastUpdated = Timestamp(marketData.last_update * 1000)
        logger.info("Event ${event.id}, market ${marketData.key}, source: ${existingMarket.source}, option ${marketOptionData.name} has been updated")
        marketOptionSink.emit(existingMarketOption)
      } else {
        // is this a valid case?
      }
      marketRepository.save(existingMarket)
    }
  }

  private fun createMarket(
    event: Event,
    marketData: MarketData,
    bookmaker: Bookmaker
  ) {
    val market = Market(
      isLive = event.isLive,
      lastUpdated = Timestamp(marketData.last_update * 1000),
      name = marketData.key,
      event = event,
      source = bookmaker.key
    )
    val savedMarket = marketRepository.save(market)

    marketData.outcomes.forEach { marketOptionData ->
      val marketOption = MarketOption(
        name = marketOptionData.name,
        odds = BigDecimal(marketOptionData.price),
        market = savedMarket,
        lastUpdated = Timestamp(marketData.last_update * 1000)
      )
      marketOptionRepository.save(marketOption)
    }
  }

  fun saveScores(eventDataWithScores: EventData, event: Event) {
    if (eventDataWithScores.scores != null) {
      eventDataWithScores.scores!!.forEach { scoreData ->
        val existingScores = scoreUpdateRepository.findByEventId(event.id)
        val score = ScoreUpdate(
          name = scoreData.name,
          score = scoreData.score,
          event = event,
          timestamp = Timestamp(Date().time)
        )
        if (existingScores.isNotEmpty()) {
          if (existingScores.find { it.name == scoreData.name && it.score == scoreData.score } == null) {
            // we don't have this score yet, create it
            logger.info("Event ${event.id}, has new score ${scoreData.name} ${scoreData.score}")
            event.scoreUpdates.add(score)
            val saved: Event = eventRepository.save(event)
            scoreUpdatesSink.emit(saved)
          } else {
            logger.debug("Score ${scoreData.name} ${scoreData.score} already exists, skipping...")
          }
        } else {
          // no existing scores
          scoreUpdateRepository.save(score)
        }
      }
    }
  }

  fun importSports(sports: List<SportData>) {
    sports.forEach { sportData ->
      val existing = sportRepository.findByKey(sportData.key)
      if (existing != null) {
        logger.debug("Sport ${sportData.key} already exists, updating active value...")
        existing.active = sportData.active
        sportRepository.save(existing)
      } else {
        val newSport = Sport(
          key = sportData.key,
          active = sportData.active,
          title = sportData.title,
          group = sportData.group,
          hasOutrights = sportData.has_outrights,
          description = sportData.description
        )
        sportRepository.save(
          newSport
        )
      }
    }
  }

  fun emitEventStatusUpdate(event: Event) {
    logger.info("Emitting event status update for event ${event.id}")
    eventStatusUpdatesSink.emit(event)
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

  suspend fun fetchEventScores(event: Event): EventData? {
    val response: HttpResponse =
      httpClient.request(getScoresApiURL(event.sport.key) + "&eventIds=${event.externalId}&daysFrom=1") {
        method = HttpMethod.Get
      }
    if (response.status != HttpStatusCode.OK) {
      throw IllegalStateException("Failed to fetch scores for one event: ${response.status}: ${response.bodyAsText()}")
    }
    val events = Json.decodeFromString(ListSerializer(EventData.serializer()), response.bodyAsText())
    return events.firstOrNull()
  }

  suspend fun updateCompleted(eventId: Int) {
    val event: Event = withContext(Dispatchers.IO) {
      eventRepository.findById(eventId)
    }.get()
    event.completed = event.startTime.before(Date())
    logger.info("Updating event ${event.id} completed to ${event.completed}")

    withContext(Dispatchers.IO) {
      eventRepository.save(event)
    }
    if (event.completed!!) {
      updateScores(event)
      withContext(Dispatchers.IO) {
        updateEventResult(event)
      }
      withContext(Dispatchers.IO) {
        emitEventStatusUpdate(event)
      }
    }
  }

  @Transactional
  fun updateResult(eventId: Int) {
    val event =
      eventRepository.findById(eventId).getOrNull()
        ?: throw Error("Cannot find event with id $eventId")
    updateEventResult(event)
  }

  @Transactional
  fun updateEventResult(event: Event) {
    logger.info("Updating result for event ${event.id}")
    val h2hMarket: Market? = event.markets.find { it.name == "h2h" }
    if (h2hMarket == null) {
      logger.info("Cannot find h2h market for event with id ${event.id}")
      return
    }

    val homeTeamScore =
      scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.homeTeamName)
    val awayTeamScore =
      scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.awayTeamName)

    val winner: EventResult = when {
      (homeTeamScore?.score?.toInt() ?: 0) > (awayTeamScore?.score?.toInt() ?: 0) -> EventResult.HOME_TEAM_WIN
      (homeTeamScore?.score?.toInt() ?: 0) < (awayTeamScore?.score?.toInt() ?: 0) -> EventResult.AWAY_TEAM_WIN
      else -> EventResult.DRAW
    }
    event.completed = true
    event.result = winner
    eventRepository.save(event)

    betService.setResults(event, h2hMarket, winner)
  }

  suspend fun updateScores(event: Event) {
    logger.info("Event ${event.id} is completed, fetching final scores")
    val eventDataWithScores = fetchEventScores(event)
    logger.info("saving final scores for event ${event.id}")
    if (eventDataWithScores != null) {
      withContext(Dispatchers.IO) {
        saveScores(eventDataWithScores, event)
      }
    }
  }

}
