package com.npd.betting.importer

import com.npd.betting.model.Event
import com.npd.betting.model.Market
import com.npd.betting.model.MarketOption
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.MarketOptionRepository
import com.npd.betting.repositories.MarketRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp

@Service
class EventService(
  private val eventRepository: EventRepository,
  private val marketRepository: MarketRepository,
  private val marketOptionRepository: MarketOptionRepository,
) {
  suspend fun importEvents(eventsData: List<EventData>) {
    eventsData.forEach { eventData ->
      val existing = eventRepository.findByExternalId(eventData.id)
      if (existing != null) {
        if (existing.isLive != eventData.isLive()) {
          println("Event ${eventData.id} is now ${if (eventData.isLive()) "live" else "not live"}")
          existing.isLive = eventData.isLive()
          eventRepository.save(existing)
        } else {
          println("Event ${eventData.id} already exists, skipping...")
        }
      } else {
        saveEvent(eventData)
      }
    }
  }

  fun saveEvent(
    eventData: EventData
  ) {
    val event = eventRepository.save(
      Event(
        isLive = eventData.isLive(),
        name = "${eventData.home_team} vs ${eventData.away_team}",
        startTime = Timestamp(eventData.commence_time.time),
        sport = eventData.sport_title,
        externalId = eventData.id
      )
    )

    eventData.bookmakers.forEach { bookmaker ->
      bookmaker.markets.forEach { marketData ->
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
    }
  }
}
