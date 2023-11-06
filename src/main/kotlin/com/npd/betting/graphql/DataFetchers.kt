package com.npd.betting.graphql

import com.npd.betting.controllers.AccumulatingSink
import com.npd.betting.model.Event
import com.npd.betting.repositories.EventRepository
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class EventDataFetcher(
  private val eventRepository: EventRepository
) : DataFetcher<List<Event>> {
  override fun get(environment: DataFetchingEnvironment): List<Event> {
    return eventRepository.findByIsLiveFalseAndCompletedFalse()
  }
}

@Component
class LiveEventDataFetcher(val eventRepository: EventRepository) : DataFetcher<List<Event>> {
  override fun get(environment: DataFetchingEnvironment): List<Event> {
    return eventRepository.findByIsLiveTrueAndCompletedFalse()
  }
}

@Component
class EventScoreUpdatesDataFetcher(val scoreUpdatesSink: AccumulatingSink<Event>) : DataFetcher<Publisher<List<Event>>> {
  override fun get(environment: DataFetchingEnvironment): Publisher<List<Event>> {
    return scoreUpdatesSink.asFlux()
  }
}