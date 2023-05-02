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
            // TODO: check if event already exists
            val event = Event(
                isLive = eventData.isLive(),
                name = "${eventData.home_team} vs ${eventData.away_team}",
                startTime = Timestamp(eventData.commence_time.time),
                sport = eventData.sport_title
            )
            val savedEvent = eventRepository.save(event)

            eventData.bookmakers.forEach { bookmaker ->
                bookmaker.markets.forEach { marketData ->
                    val market = Market(
                        isLive = event.isLive,
                        lastUpdated = Timestamp(marketData.last_update.time),
                        name = "${bookmaker.title} - ${marketData.key}",
                        event = savedEvent
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
}
