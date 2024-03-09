package com.npd.betting.services

import com.npd.betting.model.Event
import com.npd.betting.model.EventResult
import com.npd.betting.model.ScoreUpdate
import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.MarketRepository
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
  private val betService: BetService,
  private val marketRepository: MarketRepository
) {
  val logger: Logger = LoggerFactory.getLogger(EventService::class.java)


  @Transactional
  fun updateResult(eventId: Int) {
    val event =
      eventRepository.findById(eventId).getOrNull()
        ?: throw Error("Cannot find event with id $eventId")
    saveEventResult(event)
    saveMatchTotalsResult(event, event.completed ?: false)

  }

  @Transactional
  fun saveEventResult(event: Event) {
    logger.info("Updating result for event ${event.id}")
    if (event.completed == true) {
      saveH2HResult(event)
      saveSpreadsResult(event)
    }
    saveMatchTotalsResult(event, event.completed ?: false)
  }

  private fun saveSpreadsResult(event: Event) {
    val spreadMarkets = marketRepository.findByEventId(event.id).filter { it.name == "spreads" }
    if (spreadMarkets.isEmpty()) {
      logger.info("Cannot find spreads market for event with id ${event.id}")
      return
    } else {
      logger.info("Found spreads markets ${spreadMarkets.map { it.id }} for event with id ${event.id}")
    }
    spreadMarkets.forEach { spreadMarket ->
      val homeTeamOption = spreadMarket.options.find { it.name == event.homeTeamName }
      val awayTeamOption = spreadMarket.options.find { it.name == event.awayTeamName }
      if (homeTeamOption == null || awayTeamOption == null) {
        logger.error("Cannot find home or away team option for spread market ${spreadMarket.id}")
        return
      }
      logger.debug(
        "Found home team option {} with handicap {} and away team option {} with handicap {} for spread market {} for event {}",
        homeTeamOption.id,
        homeTeamOption.point,
        awayTeamOption.id,
        awayTeamOption.point,
        spreadMarket.id,
        event.id
      )
      val homeTeamScore =
        (scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.homeTeamName)?.score?.toInt() ?: 0) + (homeTeamOption.point?.toDouble() ?: 0.0)
      val awayTeamScore =
        (scoreUpdateRepository.findFirstByEventIdAndNameOrderByTimestampDesc(event.id, event.awayTeamName)?.score?.toInt() ?: 0) + (awayTeamOption.point?.toDouble() ?: 0.0)
      logger.debug("Calculated handicapped home team score: $homeTeamScore and away team score: $awayTeamScore for event ${event.id}")

      val winner = when {
        homeTeamScore > awayTeamScore -> EventResult.HOME_TEAM_WIN
        homeTeamScore < awayTeamScore -> EventResult.AWAY_TEAM_WIN
        else -> EventResult.DRAW
      }
      betService.setSpreadResult(event, spreadMarket, winner)
    }
  }

  private fun saveH2HResult(event: Event) {
    val h2hMarkets = marketRepository.findByEventId(event.id).filter { it.name == "h2h" }
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
  fun saveMatchTotalsResult(event: Event, isFinalResult: Boolean) {
    val markets = marketRepository.findByEventId(event.id).filter { it.name == "totals" }
    if (markets.isEmpty()) {
      logger.info("No totals market found for event ${event.id}")
      return
    }
    val totalScore = getTotalNumberOfScores(event)

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
    event: Event
  ): Int {
    val scores = scoreUpdateRepository.findByEventId(event.id)
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