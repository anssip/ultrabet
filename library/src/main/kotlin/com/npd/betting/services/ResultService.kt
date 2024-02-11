package com.npd.betting.services

import com.npd.betting.model.Event
import com.npd.betting.model.EventResult
import com.npd.betting.model.ScoreUpdate
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.ScoreUpdateRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional
class ResultService(
  private val eventRepository: EventRepository,
  private val scoreUpdateRepository: ScoreUpdateRepository,
  private val betService: BetService
) {
  val logger: Logger = LoggerFactory.getLogger(EventService::class.java)


  @Transactional
  fun updateResult(eventId: Int) {
    val event =
      eventRepository.findById(eventId).getOrNull()
        ?: throw Error("Cannot find event with id $eventId")
    saveEventResult(event)
    saveMatchTotalsResult(event, event.scoreUpdates, event.completed ?: false)
  }

  @Transactional
  fun saveEventResult(event: Event) {
    logger.info("Updating result for event ${event.id}")
    saveH2HResult(event)
    saveMatchTotalsResult(event, event.scoreUpdates, event.completed ?: false)
  }

  private fun saveH2HResult(event: Event) {
    val h2hMarkets = event.markets.filter { it.name == "h2h" }
    if (h2hMarkets.isEmpty()) {
      logger.info("Cannot find h2h market for event with id ${event.id}")
      return
    } else {
      logger.info("Found h2h markets ${h2hMarkets.map { it.id }} for event with id ${event.id}")
    }

    val homeTeamScore =
      scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.homeTeamName)
    val awayTeamScore =
      scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.awayTeamName)

    val winner: EventResult = when {
      (homeTeamScore?.score?.toInt() ?: 0) > (awayTeamScore?.score?.toInt() ?: 0) -> EventResult.HOME_TEAM_WIN
      (homeTeamScore?.score?.toInt() ?: 0) < (awayTeamScore?.score?.toInt() ?: 0) -> EventResult.AWAY_TEAM_WIN
      else -> EventResult.DRAW
    }
    event.completed = true
    event.result = winner
    eventRepository.save(event)

    h2hMarkets.forEach { h2hMarket ->
      betService.setH2HResults(event, h2hMarket, winner)
    }
  }

  @Transactional
  fun saveMatchTotalsResult(event: Event, scores: List<ScoreUpdate>, isFinalResult: Boolean) {
    val markets = event.markets.filter { it.name == "totals" }
    if (markets.isEmpty()) {
      logger.info("No totals market found for event ${event.id}")
      return
    }
    val totalScore = getTotalNumberOfScores(scores, event)

    markets.forEach {
      val over = it.options.find { option -> option.name == "Over" }
      // compare total score with over point as decimal numbers
      val result = if (totalScore < (over!!.point?.toDouble() ?: 0.0)) {
        EventResult.UNDER
      } else {
        EventResult.OVER
      }
      logger.info("Totals result for event ${event.id} is ${result.name}, (total scores: $totalScore, over point: ${over.point}, final result? $isFinalResult)")
      if (isFinalResult || result == EventResult.OVER) {
        // either the game is complete or we already went over the point
        betService.setTotalsResult(event, it, result)
      }
    }
  }

  private fun getTotalNumberOfScores(
    scores: List<ScoreUpdate>,
    event: Event
  ): Int {
    if (scores.isEmpty()) {
      logger.info("No scores found for event ${event.id}")
      return 0
    }
    val homeScore =
      scores.filter { it.name == event.homeTeamName }.maxByOrNull { it.score.toInt() }?.score?.toInt() ?: 0
    val awayScore =
      scores.filter { it.name == event.awayTeamName }.maxByOrNull { it.score.toInt() }?.score?.toInt() ?: 0
    return homeScore + awayScore
  }
}