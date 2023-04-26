package com.npd.betting.controllers

import com.npd.betting.model.Market
import com.npd.betting.model.MarketOption
import com.npd.betting.repositories.MarketRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class MarketController @Autowired constructor(
    private val marketRepository: MarketRepository,
    private val entityManager: EntityManager
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

    @SchemaMapping(typeName = "Market", field = "options")
    fun getMarketOptions(market: Market): List<MarketOption> {
        val query = entityManager.createQuery(
            "SELECT m FROM Market m JOIN FETCH m.options WHERE m.id = :id", Market::class.java
        )
        query.setParameter("id", market.id)
        val resultList = query.resultList
        return if (resultList.isEmpty()) emptyList() else resultList[0].options
    }
}
