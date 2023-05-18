package com.npd.betting.controllers

import com.npd.betting.model.Market
import com.npd.betting.model.MarketOption
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.MarketOptionRepository
import com.npd.betting.repositories.MarketRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

@Controller
class MarketController @Autowired constructor(
  private val marketRepository: MarketRepository,
  private val marketOptionRepository: MarketOptionRepository,
  private val eventRepository: EventRepository,
  private val entityManager: EntityManager,
) {
  private var lastPollTime: Timestamp = Timestamp.from(Instant.now())

  @SchemaMapping(typeName = "Query", field = "getMarket")
  fun getMarket(@Argument id: Int): Market {
    return marketRepository.findById(id).orElse(null)
  }

  @SchemaMapping(typeName = "Query", field = "listMarkets")
  fun listMarkets(@Argument eventId: Int): List<Market> {
    return marketRepository.findByEventId(eventId)
  }

  @SchemaMapping(typeName = "Query", field = "listLiveMarkets")
  fun listLiveMarkets(@Argument eventId: Int): List<Market> {
    return marketRepository.findByEventIdAndIsLiveTrue(eventId)
  }

  @SchemaMapping(typeName = "Market", field = "options")
  fun getMarketOptions(market: Market): List<MarketOption> {
    val query = entityManager.createQuery(
      "SELECT m FROM Market m JOIN FETCH m.options WHERE m.id = :id", Market::class.java
    )
    query.setParameter("id", market.id)
    val resultList = query.resultList
    return if (resultList.isEmpty()) emptyList() else resultList[0].options
  }

  @MutationMapping
  fun createMarket(@Argument name: String, @Argument eventId: Int): Market {
    val event = eventRepository.findById(eventId).orElse(null)
    val market = Market(name = name, event = event, isLive = false, source = "internal")
    marketRepository.save(market)
    return market
  }

  @MutationMapping
  fun createMarketOption(@Argument name: String, @Argument odds: Float, @Argument marketId: Int): MarketOption {
    val market = marketRepository.findById(marketId).orElse(null)
    val marketOption = MarketOption(name = name, odds = BigDecimal.valueOf(odds.toDouble()), market = market)
    marketOptionRepository.save(marketOption)
    return marketOption
  }

  @SubscriptionMapping
  fun liveMarketOptionUpdated(): Flux<MarketOption> {
    // Set up an interval for polling the database.
    return Flux.interval(Duration.ofSeconds(1))
      .flatMap {
        Mono.fromCallable {
//          println("Polling for updated market options updated after $lastPollTime")
          val updated = marketOptionRepository.findAllByLastUpdatedAfter(Timestamp(lastPollTime.time))
//          if (updated.size > 0) {
//            println("Found ${updated.size} updated market options!!!!")
//          }
          updated
        }
          .subscribeOn(Schedulers.boundedElastic())
          .flatMapIterable { it } // transform the list to a Flux stream
      }
      .doOnNext {
        // Update the last poll time.
        lastPollTime = Timestamp.from(Instant.now())
      }
  }

}
