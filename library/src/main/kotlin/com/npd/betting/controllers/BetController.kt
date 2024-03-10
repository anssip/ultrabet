package com.npd.betting.controllers

import com.npd.betting.model.Bet
import com.npd.betting.model.BetOption
import com.npd.betting.model.BetStatus
import com.npd.betting.repositories.*
import com.npd.betting.services.EventService
import com.npd.betting.services.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

class NotFoundException(message: String) : RuntimeException(message)
class InsufficientFundsException(message: String) : RuntimeException(message)
class InvalidBetStatusException(message: String) : RuntimeException(message)


class BetOptionInput(
  val marketOptionId: Int,
  val stake: BigDecimal
)

@Controller
open class BetController @Autowired constructor(
  private val betRepository: BetRepository,
  private val userRepository: UserRepository,
  private val marketOptionRepository: MarketOptionRepository,
  private val betOptionRepository: BetOptionRepository,
  private val walletRepository: WalletRepository,
  private val userService: UserService
) {
  val logger: Logger = LoggerFactory.getLogger(BetController::class.java)

  @SchemaMapping(typeName = "Query", field = "getBet")
  fun getBet(@Argument id: Int): Bet? {
    return betRepository.findById(id).orElse(null)
  }

  @SchemaMapping(typeName = "Query", field = "listBets")
  fun listBets(): List<Bet> {
    val user = userService.findAuthenticatedUser()
    logger.debug("Fetching bets for user ${user.id}")
    return betRepository.findByUserIdOrderByCreatedAtDesc(user)
  }

  @SchemaMapping(typeName = "Bet", field = "potentialWinnings")
  fun potentialWinnings(bet: Bet): BigDecimal {
    return bet.calculatePotentialWinnings()
  }

  @SchemaMapping(typeName = "Mutation", field = "placeBet")
  @Transactional
  open fun placeBet(
    @Argument("betType") betType: String,
    @Argument("marketOptions") marketOptionIds: List<Int>,
    @Argument("stake") stake: BigDecimal
  ): Bet {
    val user = userService.findAuthenticatedUser()

    user.wallet.let { wallet ->
      if (wallet == null) {
        throw RuntimeException("User has no wallet")
      }
      if (wallet.balance < stake) {
        throw InsufficientFundsException("Insufficient funds")
      }
    }

    val marketOptions = marketOptionRepository.findAllById(marketOptionIds)
    if (marketOptions.size != marketOptionIds.size) {
      throw NotFoundException("Some market options not found")
    }

    val bet = Bet(user, stake, BetStatus.PENDING)
    val savedBet = betRepository.save(bet) // Save the Bet entity first to generate the ID

    marketOptions.forEach { marketOption ->
      val betOption = BetOption(savedBet, marketOption) // Use the savedBet with the generated ID
      betOptionRepository.save(betOption)
      savedBet.betOptions.add(betOption) // Add the BetOption entity to the savedBet's betOptions
    }

    user.wallet.let { wallet ->
      wallet!!.balance -= stake
      walletRepository.save(wallet!!)
    }
    return savedBet
  }

  @SchemaMapping(typeName = "Mutation", field = "placeSingleBets")
  @Transactional
  open fun placeSingleBets(
    @Argument("options") options: List<BetOptionInput>
  ): List<Bet> {
    val user = userService.findAuthenticatedUser()

    user.wallet.let { wallet ->
      if (wallet == null) {
        throw RuntimeException("User has no wallet")
      }
    }

    return options.map { option ->
      val bet = Bet(user, option.stake, BetStatus.PENDING)
      val savedBet = betRepository.save(bet) // Save the Bet entity first to generate the ID

      val marketOption = marketOptionRepository.findById(option.marketOptionId)
      if (marketOption.isEmpty) {
        throw NotFoundException("Market option not found")
      }

      user.wallet.let { wallet ->
        if (wallet!!.balance < option.stake) {
          throw InsufficientFundsException("Insufficient funds")
        }
        wallet.balance -= option.stake
        walletRepository.save(wallet)
      }

      val betOption = BetOption(savedBet, marketOption.get()) // Use the savedBet with the generated ID
      betOptionRepository.save(betOption)
      savedBet.betOptions.add(betOption)
      savedBet
    }
  }
}
