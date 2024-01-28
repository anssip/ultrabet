package com.npd.betting.services

import com.npd.betting.model.*
import com.npd.betting.repositories.BetOptionRepository
import com.npd.betting.repositories.BetRepository
import com.npd.betting.repositories.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
@Transactional
class BetService(
  private val betRepository: BetRepository,
  private val betOptionRepository: BetOptionRepository,
  private val walletRepository: WalletRepository
) {
  val logger: Logger = LoggerFactory.getLogger(BetService::class.java)

  fun setResults(
    event: Event,
    h2hMarket: Market,
    winner: EventResult,
  ) {
    logger.info("Setting results for event ${event.id} with winner ${winner.name}")
    val winningMarketOption: MarketOption? = h2hMarket.options.find {
      it.name == when (winner) {
        EventResult.HOME_TEAM_WIN -> event.homeTeamName
        EventResult.AWAY_TEAM_WIN -> event.awayTeamName
        else -> "Draw"
      }
    }
    if (winningMarketOption != null) {
      logger.info("Winning market option has id '${winningMarketOption.id}' Updating all bet options having this market option with status WON")
      val result = betOptionRepository.updateAllByMarketOptionId(winningMarketOption.id, BetStatus.WON)
      logger.info("Updated ${result.toString()} bet options")

      val losingMarketOptions = h2hMarket.options.filter { it.name != winningMarketOption.name }
      logger.info("Updating all bet options having these market options with status LOST: ${losingMarketOptions.map { it.id }}")
      losingMarketOptions.forEach {
        betOptionRepository.updateAllByMarketOptionId(it.id, BetStatus.LOST)
      }
    betOptionRepository.flush()
    } else {
      logger.warn("Winning market option for winner '${winner.name}' not found for market with id ${h2hMarket.id}. All bets will loose!")

      h2hMarket.options.forEach {
        betOptionRepository.updateAllByMarketOptionId(it.id, BetStatus.LOST)
      }
    }

    val winningBets = betRepository.findBetsWithWinningOptions(BetStatus.PENDING)
    logger.info("Found ${winningBets.size} winning bets")
    winningBets.forEach {
      it.status = BetStatus.WON
      betRepository.save(it)
      // pay out winnings
      val wallet = it.user.wallet ?: throw Error("Wallet not found for user ${it.user.id}")
      wallet.balance += it.calculatePotentialWinnings()
      walletRepository.save(wallet)
    }
    betRepository.updateAllLosing()
  }

}