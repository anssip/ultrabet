package com.npd.betting.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "username", unique = true, nullable = false)
    val username: String,

    @Column(name = "email", unique = true, nullable = false)
    val email: String,

    @Column(name = "password", nullable = false)
    val password: String,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val wallets: List<Wallet> = emptyList(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val bets: List<Bet> = emptyList()
)

@Entity
@Table(name = "wallets")
data class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "balance", nullable = false)
    val balance: BigDecimal,

    @OneToMany(mappedBy = "wallet", cascade = [CascadeType.ALL])
    val transactions: List<Transaction> = emptyList()
)

@Entity
@Table(name = "transactions")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "wallet_id")
    val wallet: Wallet,

    @Column(name = "amount", nullable = false)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    val transactionType: TransactionType,

    @Column(name = "created_at", nullable = false)
    val createdAt: Timestamp = Timestamp(System.currentTimeMillis())
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    BET_PLACED,
    BET_WON,
    BET_REFUNDED
}

@Entity
@Table(name = "events")
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "is_live", nullable = false)
    val isLive: Boolean,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "start_time", nullable = false)
    val startTime: Timestamp,

    @Column(name = "sport", nullable = false)
    val sport: String,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL])
    val markets: List<Market> = emptyList()
)

@Entity
@Table(name = "markets")
data class Market(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "is_live", nullable = false)
    val isLive: Boolean,

    @Column(name = "last_updated")
    val lastUpdated: Timestamp? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @ManyToOne
    @JoinColumn(name = "event_id")
    val event: Event,

    @OneToMany(mappedBy = "market", cascade = [CascadeType.ALL])
    val options: List<MarketOption> = emptyList()
)

@Entity
@Table(name = "market_options")
data class MarketOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "last_updated")
    val lastUpdated: Timestamp? = null,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "odds", nullable = false)
    val odds: BigDecimal,

    @ManyToOne
    @JoinColumn(name = "market_id")
    val market: Market,

    @OneToMany(mappedBy = "marketOption", cascade = [CascadeType.ALL])
    val bets: List<Bet> = emptyList()
)

@Entity
@Table(name = "bets")
data class Bet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User,

    @ManyToOne
    @JoinColumn(name = "market_option_id")
    val marketOption: MarketOption,

    @Column(name = "stake", nullable = false)
    val stake: BigDecimal,

    @Column(name = "potential_winnings", nullable = false)
    val potentialWinnings: BigDecimal,

    @Column(name = "created_at", nullable = false)
    val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: BetStatus
)

enum class BetStatus {
    PENDING,
    WON,
    LOST,
    CANCELED
}
