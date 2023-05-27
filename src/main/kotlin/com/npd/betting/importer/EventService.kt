package com.npd.betting.importer

import com.npd.betting.controllers.AccumulatingSink
import com.npd.betting.model.*
import com.npd.betting.repositories.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val marketRepository: MarketRepository,
  private val marketOptionRepository: MarketOptionRepository,
  private val scoreUpdateRepository: ScoreUpdateRepository,
  private val sportRepository: SportRepository,
  private val marketOptionSink: AccumulatingSink<MarketOption>
) {
  val logger: Logger = LoggerFactory.getLogger(EventService::class.java)

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
          println("Event ${eventData.id} is now ${if (eventData.isLive()) "live" else "not live"}")
          existing.isLive = eventData.isLive()
          existing.completed = eventData.completed ?: existing.completed
          eventRepository.save(existing)
        } else {
          logger.debug("Event ${eventData.id} already exists, skipping...")
        }
      } else {
        saveEventAndOdds(eventData)
      }
    }
  }

  fun saveEventAndOdds(
    eventData: EventData,
    update: Boolean = false
  ) {

    val event = if (update) {
      val existing = eventRepository.findByExternalId(eventData.id)
        ?: throw Exception("Event ${eventData.id} does not exist")
      existing.isLive = eventData.isLive()
      existing.completed = eventData.completed ?: existing.completed
      eventRepository.save(
        existing
      )
    } else {
      val sportEntity = sportRepository.findByKey(eventData.sport_key)
        ?: throw Exception("Sport with key ${eventData.sport_key} does not exist")

      val newEvent = Event(
        isLive = eventData.isLive(),
        name = "${eventData.home_team} vs ${eventData.away_team}",
        startTime = Timestamp(eventData.commence_time * 1000),
        sport = sportEntity,
        externalId = eventData.id
      )
      if (eventData.completed != null) {
        newEvent.completed = eventData.completed ?: newEvent.completed
      }
      eventRepository.save(
        newEvent
      )
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
            scoreUpdateRepository.save(score)
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

  fun updateCompleted(eventId: Int) {
    val event = eventRepository.findById(eventId).get()
    event.completed = event.startTime.before(Date())
    logger.info("Updating event ${event.id} completed to ${event.completed}")
    eventRepository.save(event)
  }
}
