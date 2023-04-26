package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.Market
import com.npd.betting.repositories.EventRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class EventController @Autowired constructor(
    private val eventRepository: EventRepository,
    private val entityManager: EntityManager
) {

    @SchemaMapping(typeName = "Query", field = "getEvent")
    fun getEvent(@Argument id: Int): Event {
        return eventRepository.findById(id).orElse(null)
    }

    @SchemaMapping(typeName = "Query", field = "listEvents")
    fun listEvents(): List<Event> {
        return eventRepository.findAll()
    }

    @SchemaMapping(typeName = "Query", field = "listLiveEvents")
    fun listLiveEvents(): List<Event> {
        return eventRepository.findByIsLiveTrue()
    }

    @SchemaMapping(typeName = "Event", field = "markets")
    fun getEventMarkets(event: Event): List<Market> {
        val query = entityManager.createQuery(
            "SELECT e FROM Event e JOIN FETCH e.markets WHERE e.id = :id", Event::class.java
        )
        query.setParameter("id", event.id)
        val resultList = query.resultList
        return if (resultList.isEmpty()) emptyList() else resultList[0].markets
    }
}
