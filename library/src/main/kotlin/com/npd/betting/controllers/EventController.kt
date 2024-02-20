package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.Market
import com.npd.betting.model.ScoreUpdate
import com.npd.betting.model.Sport
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.SportRepository
import com.npd.betting.services.ResultService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.sql.Timestamp
import java.time.LocalDateTime

@Controller
class EventController @Autowired constructor(
  private val eventRepository: EventRepository,
  private val sportRepository: SportRepository,
  private val resultService: ResultService,
  private val entityManager: EntityManager,
  private val scoreUpdatesSink: AccumulatingSink<Event>,
  private val eventStatusUpdatesSink: AccumulatingSink<Event>
) {

  @SchemaMapping(typeName = "Query", field = "listSports")
  fun getSports(): List<Sport> {
    return sportRepository.findByActiveTrue()
  }

  @SchemaMapping(typeName = "Sport", field = "events")
  fun getSportEvents(sport: Sport): List<Event> {
    return eventRepository.findBySportIdAndCompletedFalse(sport.id)
  }

  @SchemaMapping(typeName = "Sport", field = "activeEventCount")
  fun activeEventCount(sport: Sport): Int {
    return eventRepository.countBySportIdAndCompleted(sport.id, false)
  }

  @SchemaMapping(typeName = "Query", field = "getEvent")
  fun getEvent(@Argument id: Int): Event? {
    return eventRepository.findById(id).orElse(null)
  }

  @SchemaMapping(typeName = "Query", field = "listEvents")
  fun listEvents(): List<Event> {
    return eventRepository.findByIsLiveFalseAndCompletedFalse()
  }

  @SchemaMapping(typeName = "Query", field = "listAllEvents")
  fun listAllEvents(): List<Event> {
    return eventRepository.findByCompletedFalse()
  }

  @SchemaMapping(typeName = "Query", field = "listLiveEvents")
  fun listLiveEvents(): List<Event> {
    return eventRepository.findByIsLiveTrueAndCompletedFalse()
  }

  @SchemaMapping(typeName = "Event", field = "markets")
  fun getEventMarkets(event: Event, @Argument("source") source: String?): List<Market> {
    val sourceClause = if (source != null) "AND m.source = :source" else ""
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

  @SchemaMapping(typeName = "Event", field = "scoreUpdates")
  fun getEventScoreUpdates(event: Event): List<ScoreUpdate> {
    val query = entityManager.createQuery(
      "SELECT e FROM Event e JOIN FETCH e.scoreUpdates s WHERE e.id = :id", Event::class.java
    )
    query.setParameter("id", event.id)
    val resultList = query.resultList
    return if (resultList.isEmpty()) emptyList() else resultList[0].scoreUpdates

  }

  @MutationMapping
  fun createEvent(
    @Argument homeTeamName: String,
    @Argument awayTeamName: String,
    @Argument name: String,
    @Argument startTime: String,
    @Argument sport: String
  ): Event {
    val sportEntity = sportRepository.findByKey(sport) ?: throw Exception("Sport with key $sport does not exist")
    val event = Event(
      homeTeamName = homeTeamName,
      awayTeamName = awayTeamName,
      name = name,
      startTime = Timestamp.valueOf(LocalDateTime.parse(startTime)),
      sport = sportEntity,
      isLive = false,
      completed = false
    )
    eventRepository.save(event)
    return event
  }

  @SchemaMapping(typeName = "Mutation", field = "updateResult")
  fun updateResult(@Argument("eventId") eventId: Int) {
    resultService.updateResult(eventId)
  }

  @SubscriptionMapping
  fun eventScoresUpdated(): Flux<List<Event>> {
    return scoreUpdatesSink.asFlux()
  }

  @SubscriptionMapping
  fun eventStatusUpdated(): Flux<List<Event>> {
    return eventStatusUpdatesSink.asFlux()
  }
}
