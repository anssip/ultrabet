package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.Market
import com.npd.betting.repositories.EventRepository
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.sql.Timestamp
import java.time.LocalDateTime

@Controller
class EventController @Autowired constructor(
  private val eventRepository: EventRepository,
  private val entityManager: EntityManager
) {

  @SchemaMapping(typeName = "Query", field = "getEvent")
  fun getEvent(@Argument id: Int): Event? {
    return eventRepository.findById(id).orElse(null)
  }

  @SchemaMapping(typeName = "Query", field = "listEvents")
  fun listEvents(): List<Event> {
    return eventRepository.findAll()
  }

  @SchemaMapping(typeName = "Query", field = "listLiveEvents")
  fun listLiveEvents(): List<Event> {
    return eventRepository.findByIsLiveTrueAndCompletedFalse()
  }

  @SchemaMapping(typeName = "Event", field = "markets")
  fun getEventMarkets(event: Event, @Argument("source") source: String?): List<Market> {
    val sourceClause = if (source != null) "AND m.source = :source" else "";
    val query = entityManager.createQuery(
      "SELECT e FROM Event e JOIN FETCH e.markets m WHERE e.id = :id $sourceClause", Event::class.java
    )
    query.setParameter("id", event.id)
    if (source != null) {
      query.setParameter("source", source)
    }
    val resultList = query.resultList
    return if (resultList.isEmpty()) emptyList() else resultList[0].markets
  }


  @MutationMapping
  fun createEvent(@Argument name: String, @Argument startTime: String, @Argument sport: String): Event {
    val event = Event(
      name = name,
      startTime = Timestamp.valueOf(LocalDateTime.parse(startTime)),
      sport = sport,
      isLive = false,
      completed = false
    )
    eventRepository.save(event)
    return event
  }
}
