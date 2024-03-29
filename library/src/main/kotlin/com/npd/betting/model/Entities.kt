package com.npd.betting.model

import jakarta.persistence.*
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "users")
data class User(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "external_id", unique = true, nullable = false)
  val externalId: String,

  @Column(name = "username", unique = true, nullable = true)
  val username: String?,

  @Column(name = "email", unique = true, nullable = true)
  val email: String?,

  @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
  var wallet: Wallet? = null,

  @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
  val bets: List<Bet> = emptyList()
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as User
    return id == other.id
  }
}

@Entity
@Table(name = "wallets")
data class Wallet(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @OneToOne
  @JoinColumn(name = "user_id")
  val user: User,

  @Column(name = "balance", nullable = false)
  var balance: BigDecimal,

  @OneToMany(mappedBy = "wallet", cascade = [CascadeType.ALL])
  val transactions: List<Transaction> = emptyList()
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Wallet
    return id == other.id
  }
}

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

enum class EventResult {
  HOME_TEAM_WIN,
  DRAW,
  AWAY_TEAM_WIN,
  OVER,
  UNDER
}

@Entity
@Table(name = "events")
@NamedEntityGraph(
  name = "Event.withMarketsAndOptions",
  attributeNodes = [
    NamedAttributeNode("markets", subgraph = "Market.withOptions"),
    NamedAttributeNode("sport")
  ],
  subgraphs = [
    NamedSubgraph(
      name = "Market.withOptions",
      attributeNodes = [NamedAttributeNode("options")]
    )
  ]
)
data class Event(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "external_id", unique = true)
  val externalId: String? = null,

  @Column(name = "is_live", nullable = false)
  var isLive: Boolean,

  @Column(name = "completed", nullable = false)
  var completed: Boolean? = false,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "homeTeamName", nullable = false)
  var homeTeamName: String,

  @Column(name = "awayTeamName", nullable = false)
  var awayTeamName: String,

  @Column(name = "start_time", nullable = false)
  val startTime: Timestamp,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sport")
  val sport: Sport,

  @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val markets: List<Market> = emptyList(),

  @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL])
  val scoreUpdates: MutableSet<ScoreUpdate> = mutableSetOf(),

  @Enumerated(EnumType.STRING)
  @Column(name = "result")
  var result: EventResult? = null
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Event
    return id == other.id
  }

}

@Entity
@Table(name = "sports")
data class Sport(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "`key`", nullable = false)
  val key: String,

  @Column(name = "title", nullable = false)
  val title: String,

  @Column(name = "description", nullable = false)
  val description: String,

  @Column(name = "groupId", nullable = false)
  val group: String,

  @Column(name = "active", nullable = false)
  var active: Boolean,

  @Column(name = "has_outrights", nullable = false)
  val hasOutrights: Boolean,

  @OneToMany(mappedBy = "sport", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  val events: Set<Event> = emptySet()
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Sport
    return id == other.id
  }
}

@Entity
@Table(name = "markets")
data class Market(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "is_live", nullable = false)
  var isLive: Boolean,

  @Column(name = "last_updated")
  var lastUpdated: Timestamp? = null,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "source", nullable = false)
  val source: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  val event: Event,

  @OneToMany(mappedBy = "market", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val options: Set<MarketOption> = emptySet()
)

@Entity
@Table(name = "market_options")
data class MarketOption(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @Column(name = "last_updated")
  var lastUpdated: Timestamp? = null,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "odds", nullable = false)
  var odds: BigDecimal,

  @Column(name = "point")
  var point: BigDecimal? = null,

  @Column(name = "description")
  var description: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "market_id")
  val market: Market,

  @OneToMany(mappedBy = "marketOption", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val betOptions: MutableList<BetOption> = mutableListOf()
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as MarketOption
    return id == other.id
  }
}

@Entity
@Table(name = "score_updates")
data class ScoreUpdate(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int = 0,

  @ManyToOne
  @JoinColumn(name = "event_id")
  val event: Event,

  @Column(name = "score", nullable = false)
  val score: String,

  @Column(name = "name", nullable = false)
  val name: String,

  @Column(name = "timestamp", nullable = false)
  val timestamp: Timestamp
)

@Entity
@Table(name = "bets")
data class Bet(

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  val user: User,

  @Column(name = "stake", nullable = false)
  val stake: BigDecimal,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  var status: BetStatus,

  @OneToMany(mappedBy = "bet", cascade = [CascadeType.PERSIST], fetch = FetchType.LAZY)
  @Fetch(FetchMode.SUBSELECT)
  var betOptions: MutableSet<BetOption> = mutableSetOf(),

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  val id: Int? = null,

  @Column(name = "created_at", nullable = false)
  var createdAt: Timestamp,

  ) {
  constructor(user: User, stake: BigDecimal, status: BetStatus) : this(
    user,
    stake,
    status,
    mutableSetOf(),
    null,
    Timestamp(System.currentTimeMillis())
  )

  fun calculatePotentialWinnings(): BigDecimal {
    return betOptions.fold(stake) { total, betOption ->
      total * betOption.marketOption.odds
    }
  }

  fun addMarketOption(marketOption: MarketOption): BetOption {
    val betOption = BetOption(this, marketOption)
    betOptions.add(betOption)
    return betOption
  }

  fun getId(): Int {
    return id ?: 0
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Bet
    return id == other.id
  }
}

@Entity
@Table(name = "bet_options")
data class BetOption(
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bet_id")
  val bet: Bet,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "market_option_id")
//  @Fetch(FetchMode.JOIN)
  val marketOption: MarketOption,

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  var status: BetStatus? = BetStatus.PENDING,
) {
  override fun hashCode(): Int {
    return id.hashCode()
  }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as BetOption
    return id == other.id
  }
}


enum class BetStatus {
  PENDING,
  WON,
  LOST,
  CANCELED
}
