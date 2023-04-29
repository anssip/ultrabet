package com.npd.betting.controllers

import com.npd.betting.model.Bet
import com.npd.betting.model.BetOption
import com.npd.betting.model.BetStatus
import com.npd.betting.repositories.BetOptionRepository
import com.npd.betting.repositories.BetRepository
import com.npd.betting.repositories.MarketOptionRepository
import com.npd.betting.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

class NotFoundException(message: String) : RuntimeException(message)
class InsufficientFundsException(message: String) : RuntimeException(message)
class InvalidBetStatusException(message: String) : RuntimeException(message)

@Controller
class BetController @Autowired constructor(
    private val betRepository: BetRepository,
    private val userRepository: UserRepository,
    private val marketOptionRepository: MarketOptionRepository,
    private val betOptionRepository: BetOptionRepository,
) {

    @SchemaMapping(typeName = "Query", field = "getBet")
    fun getBet(@Argument id: Int): Bet? {
        return betRepository.findById(id).orElse(null)
    }

    @SchemaMapping(typeName = "Query", field = "listBets")
    fun listBets(@Argument userId: Int): List<Bet> {
        return betRepository.findByUserId(userId)
    }

    @SchemaMapping(typeName = "Bet", field = "potentialWinnings")
    fun potentialWinnings(bet: Bet): BigDecimal {
        return bet.calculatePotentialWinnings()
    }

    @SchemaMapping(typeName = "Mutation", field = "placeBet")
    @Transactional
    fun placeBet(
        @Argument("userId") userId: Int,
        @Argument("marketOptions") marketOptionIds: List<Int>,
        @Argument("stake") stake: BigDecimal
    ): Bet {
        val user = userRepository.findById(userId).orElseThrow { NotFoundException("User not found") }
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
        }
        return savedBet
    }
}
