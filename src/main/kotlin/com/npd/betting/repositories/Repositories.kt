package com.npd.betting.repositories

import com.npd.betting.model.*
import org.springframework.data.jpa.repository.JpaRepository

// default methods in JPA repiositories
// https://www.tutorialspoint.com/spring_boot_jpa/spring_boot_jpa_repository_methods.htm

interface UserRepository : JpaRepository<User, Int> {
    // Custom methods can be added here if needed
}

interface WalletRepository : JpaRepository<Wallet, Int>

interface BetRepository : JpaRepository<Bet, Int> {
    fun findByUserId(userId: Int): List<Bet>
}

interface EventRepository : JpaRepository<Event, Int> {
    fun findByIsLiveTrue(): List<Event>
}

interface MarketRepository : JpaRepository<Market, Int> {
    fun findByEventId(eventId: Int): List<Market>
    fun findByEventIdAndIsLiveTrue(eventId: Int): List<Market>
}

interface MarketOptionRepository : JpaRepository<MarketOption, Int>

interface TransactionRepository : JpaRepository<Transaction, Int>

interface BetOptionRepository : JpaRepository<BetOption, Int>