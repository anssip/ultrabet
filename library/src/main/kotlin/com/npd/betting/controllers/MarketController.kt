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
import java.math.BigDecimal

@Controller
class MarketController @Autowired constructor(
  private val marketRepository: MarketRepository,
  private val marketOptionRepository: MarketOptionRepository,
  private val eventRepository: EventRepository,
  private val entityManager: EntityManager,
  private val marketOptionSink: AccumulatingSink<MarketOption>
) {

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
  fun liveMarketOptionsUpdated(): Flux<List<MarketOption>> {
    return marketOptionSink.asFlux()
  }

}
