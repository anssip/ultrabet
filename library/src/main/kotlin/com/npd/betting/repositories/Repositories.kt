package com.npd.betting.repositories

import com.npd.betting.model.*
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.sql.Timestamp

// default methods in JPA repiositories
// https://www.tutorialspoint.com/spring_boot_jpa/spring_boot_jpa_repository_methods.htm

interface UserRepository : JpaRepository<User, Int> {
  fun findByEmail(email: String): User?
  fun findByExternalId(externalId: String?): User?
}

interface WalletRepository : JpaRepository<Wallet, Int> {
  fun findByUserId(userId: Int): Wallet?
}

interface BetRepository : JpaRepository<Bet, Int> {
  fun findByUserIdOrderByCreatedAtDesc(userId: Int, pageable: Pageable): List<Bet>

  @Modifying
  @Query("UPDATE Bet b SET b.status = com.npd.betting.model.BetStatus.WON WHERE b.status = com.npd.betting.model.BetStatus.PENDING AND 0 = (SELECT COUNT(bo) FROM BetOption bo WHERE bo.bet.id = b.id AND bo.status != com.npd.betting.model.BetStatus.WON)")
  fun updateAllWinning()

  @Modifying
  @Query("UPDATE Bet b SET b.status = com.npd.betting.model.BetStatus.LOST WHERE b.status = com.npd.betting.model.BetStatus.PENDING " +
    "AND 0 = (SELECT COUNT(bo) FROM BetOption bo WHERE bo.bet.id = b.id AND bo.status = com.npd.betting.model.BetStatus.PENDING) " +
    "AND 0 < (SELECT COUNT(bo) FROM BetOption bo WHERE bo.bet.id = b.id AND bo.status = com.npd.betting.model.BetStatus.LOST)")
  fun updateAllLosing()

  @Query("SELECT b FROM Bet b WHERE b.status = com.npd.betting.model.BetStatus.PENDING AND 0 = (SELECT COUNT(bo) FROM BetOption bo WHERE bo.bet.id = b.id AND bo.status != com.npd.betting.model.BetStatus.WON)")
  fun findAllWinning(marketOptionId: Int): List<Bet>

  @Query("SELECT b FROM Bet b WHERE b.status = :currentStatus " +
    "AND 0 = (SELECT COUNT(bo) FROM BetOption bo WHERE bo.bet.id = b.id AND bo.status != com.npd.betting.model.BetStatus.WON)")
  fun findBetsWithWinningOptions(currentStatus: BetStatus): List<Bet>
}

@Repository
interface EventRepository : JpaRepository<Event, Int> {
  fun findByIsLiveTrueAndCompletedFalse(): List<Event>
  fun findByIsLiveFalseAndCompletedFalse(): List<Event>

  fun findByCompletedFalse(): List<Event>

  fun findByExternalId(externalId: String): Event?

  fun findBySportIdAndCompletedFalse(sportId: Int): List<Event>

  fun countBySportIdAndCompleted(sportId: Int, completed: Boolean): Int

  @Query("SELECT e FROM Event e WHERE e.sport.group = :group AND e.completed = false")
  fun findBySportGroupAndCompletedFalse(group: String): List<Event>
}


interface MarketRepository : JpaRepository<Market, Int> {
  fun findByEventId(eventId: Int): List<Market>
  fun findByEventIdAndIsLiveTrue(eventId: Int): List<Market>
  fun findByEventIdAndSourceAndName(eventId: Int, source: String, name: String): Market?
}


interface MarketOptionRepository : JpaRepository<MarketOption, Int> {
  fun findAllByLastUpdatedAfter(lastUpdated: Timestamp): List<MarketOption>
  fun findByMarketId(marketId: Int): List<MarketOption>
}

interface TransactionRepository : JpaRepository<Transaction, Int>

interface BetOptionRepository : JpaRepository<BetOption, Int> {
  @Modifying
  @Query("UPDATE BetOption b SET b.status = :status WHERE b.marketOption.id = :marketOptionId")
  fun updateAllByMarketOptionId(marketOptionId: Int, status: BetStatus)
}

interface ScoreUpdateRepository : JpaRepository<ScoreUpdate, Int> {
  fun findByEventId(eventId: Int): List<ScoreUpdate>
  fun deleteByEventId(eventId: Int)
  fun findFirstByEventIdAndNameOrderByTimestampDesc(id: Int, name: String): ScoreUpdate?

}

interface SportRepository : JpaRepository<Sport, Int> {
  fun findByKey(key: String): Sport?

  fun findByActiveTrue(): List<Sport>
}
