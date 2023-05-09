package com.npd.betting.importer

import com.npd.betting.model.*
import com.npd.betting.repositories.*
import jakarta.persistence.EntityManager
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
  private val entityManager: EntityManager
) {
  suspend fun importEvents(eventsData: List<EventData>) {
    eventsData.forEach { eventData ->
      val existing = eventRepository.findByExternalId(eventData.id)
      if (existing != null) {
        if (eventData.isLive()) {
          println("Event ${eventData.id} is now live")
        }
        if (existing.isLive != eventData.isLive()) {
          println("Event ${eventData.id} is now ${if (eventData.isLive()) "live" else "not live"}")
          existing.isLive = eventData.isLive()
          existing.completed = eventData.completed ?: existing.completed
          eventRepository.save(existing)
        } else {
          println("Event ${eventData.id} already exists, skipping...")
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
        startTime = Timestamp(eventData.commence_time.time),
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

    if (eventData.bookmakers != null) {
      eventData.bookmakers!!.forEach { bookmaker ->
        bookmaker.markets.forEach { marketData ->
          val existingMarket = marketRepository.findByEventIdAndSourceAndName(event.id, bookmaker.key, marketData.key)
          if (existingMarket != null) {
            if (Date(existingMarket.lastUpdated?.time!!).before(marketData.last_update)) {
              // update market
              println("Event ${event.id}, market ${marketData.key}, source: ${bookmaker.key} has been updated")
              entityManager.remove(existingMarket)
              entityManager.flush()
              saveMarket(event, marketData, bookmaker)
            } else {
              println("Market did not change")
            }
          } else {
            saveMarket(event, marketData, bookmaker)
          }
        }
      }
    }
  }

  private fun saveMarket(
    event: Event,
    marketData: MarketData,
    bookmaker: Bookmaker
  ) {
    val market = Market(
      isLive = event.isLive,
      lastUpdated = Timestamp(marketData.last_update.time),
      name = marketData.key,
      event = event,
      source = bookmaker.key
    )
    val savedMarket = marketRepository.save(market)

    marketData.outcomes.forEach { marketOptionData ->
      val marketOption = MarketOption(
        name = marketOptionData.name,
        odds = BigDecimal(marketOptionData.price),
        market = savedMarket
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
            println("Score ${scoreData.name} ${scoreData.score} already exists, skipping...")
          }
        } else {
          // no existing scores
          scoreUpdateRepository.save(score)
        }
      }
    }
  }

  fun importSports(sports: List<SportData>) {
    sports.forEach() { sportData ->
      val existing = sportRepository.findByKey(sportData.key)
      if (existing != null) {
        println("Sport ${sportData.key} already exists, updating active value...")
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
}
