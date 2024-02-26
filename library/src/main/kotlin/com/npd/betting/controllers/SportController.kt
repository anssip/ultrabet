package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.Sport
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.SportRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class SportController @Autowired constructor(
  private val sportRepository: SportRepository,
  private val eventRepository: EventRepository
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

}