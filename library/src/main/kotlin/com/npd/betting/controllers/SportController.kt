package com.npd.betting.controllers

import com.npd.betting.model.Event
import com.npd.betting.model.Sport
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.SportRepository
import com.npd.betting.services.importer.EventImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class SportController @Autowired constructor(
  private val sportRepository: SportRepository,
  private val eventRepository: EventRepository
) {
  val logger: Logger = LoggerFactory.getLogger(SportController::class.java)

  @SchemaMapping(typeName = "Query", field = "listSports")
  fun getSports(@Argument group: String): List<Sport> {
    if (group == "" || group == "all") {
      logger.debug("Fetching all sports")
      return sportRepository.findByActiveTrue()
    }
    return sportRepository.findByGroupAndActiveTrue(group)
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