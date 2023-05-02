package com.npd.betting.importer

import com.npd.betting.repositories.EventRepository
import com.npd.betting.repositories.MarketRepository
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class EventImporterTest {
    @MockBean
    private lateinit var eventRepository: EventRepository

    @MockBean
    private lateinit var marketRepository: MarketRepository

    @Autowired
    private lateinit var eventImporter: EventImporter

    @Test
    fun `test fetchEvents`() = runBlocking {

        val mockEngine = MockEngine {
            respond(mockResponse, HttpStatusCode.OK)
        }

        val testClient = HttpClient(mockEngine) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }

        val oldClient = eventImporter.client
        eventImporter.client = testClient

        val events = eventImporter.fetchEvents()

        assertEquals(1, events.size)
        assertEquals("a20d969267d7244666dee495af9978d5", events[0].id)
        assertEquals("cricket_ipl", events[0].sport_key)
        assertEquals("IPL", events[0].sport_title)
        assertEquals("Mumbai Indians", events[0].home_team)
        assertEquals("Rajasthan Royals", events[0].away_team)
        assertEquals(2, events[0].bookmakers.size)
        val unibet = events[0].bookmakers[0]
        assertEquals("unibet_eu", unibet.key)
        assertEquals("Unibet", unibet.title)
        assertEquals(1, unibet.markets.size)
        val h2h = unibet.markets[0]
        assertEquals("h2h", h2h.key)
        assertEquals(2, h2h.outcomes.size)
        val homeWin = h2h.outcomes[0]
        assertEquals("Mumbai Indians", homeWin.name)
        assertEquals(2.6, homeWin.price)
        val awayWin = h2h.outcomes[1]
        assertEquals("Rajasthan Royals", awayWin.name)
        assertEquals(1.5, awayWin.price)

        eventImporter.client = oldClient // Restore the original client
    }
}
