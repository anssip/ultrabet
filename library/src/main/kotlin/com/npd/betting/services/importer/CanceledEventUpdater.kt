package com.npd.betting.services.importer

import com.npd.betting.repositories.EventRepository
import com.npd.betting.services.EventService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
open class CanceledEventUpdater(
  private val eventRepository: EventRepository,
  private val eventService: EventService
) {
  val logger: Logger = LoggerFactory.getLogger(CanceledEventUpdater::class.java)

  @Scheduled(fixedRate = 1 * 60000) // Poll the API every 30 minutes
  @Transactional
  open fun updateEvents() {
    logger.debug("Updating canceled events")
    runBlocking {
      updateNonStartedOldEvents()
    }
  }


  suspend fun updateNonStartedOldEvents() {
    val events = withContext(Dispatchers.IO) {
      eventRepository.findByIsLiveFalseAndCompletedFalse()
    }
    events.forEach() {
      val event = it
      val now = Date().time
      val commenceTime = event.startTime.time
      val diff = now - commenceTime
      val diffInHours = diff / (60 * 60 * 1000)
      if (diffInHours > 24) {
        logger.debug("Event ${event.id} is more than 24 hours old, marking as completed")
        event.completed = true
        eventService.updateScores(event)
        eventService.saveEventResult(event)
      }
    }
  }
}