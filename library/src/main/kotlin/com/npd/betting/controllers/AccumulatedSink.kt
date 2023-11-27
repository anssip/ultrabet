package com.npd.betting.controllers

import com.npd.betting.services.importer.EventImporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration

class AccumulatingSink<T>(
  private val bufferSize: Int = 100,
  private val duration: Duration = Duration.ofSeconds(5),
  private val clearThreshold: Int = 1000 // Adjust the threshold as needed

) {
  val logger: Logger = LoggerFactory.getLogger(EventImporter::class.java)
  private val sinkProcessor: Sinks.Many<List<T>> = Sinks.many().multicast().onBackpressureBuffer<List<T>>()
  private val accumulatedMessages = mutableListOf<T>()
  private val emittedMessages = mutableSetOf<T>()
  private var emissionCount = 0

  init {
    sinkProcessor.emitNext(emptyList(), Sinks.EmitFailureHandler.FAIL_FAST)

    sinkProcessor.asFlux().bufferTimeout(bufferSize, duration)
      .doOnNext { messages -> emitAccumulated(messages.flatten()) }
      .subscribe()
  }

  private fun emitAccumulated(messages: List<T>) {
    synchronized(accumulatedMessages) {
      if (messages.isNotEmpty()) {
        accumulatedMessages.addAll(messages)
        if (accumulatedMessages.size >= bufferSize) {
          val uniqueMessages = accumulatedMessages.filter { it !in emittedMessages }
          emitUniqueMessages(uniqueMessages)
        }
      }
    }
  }


  fun emit(message: T) {
    synchronized(accumulatedMessages) {
      accumulatedMessages.add(message)
      if (accumulatedMessages.size >= bufferSize) {
        val uniqueMessages = accumulatedMessages.filter { it !in emittedMessages }
        logger.info("Emitting ${uniqueMessages.size} messages")
        emitUniqueMessages(uniqueMessages)
      }
    }
  }

  fun complete() {
    synchronized(accumulatedMessages) {
      if (accumulatedMessages.isNotEmpty()) {
        val uniqueMessages = accumulatedMessages.filter { it !in emittedMessages }
        emitUniqueMessages(uniqueMessages)
      }
      sinkProcessor.tryEmitComplete()
    }
  }

  private fun emitUniqueMessages(messages: List<T>) {
    accumulatedMessages.clear()
    emittedMessages.addAll(messages)
    sinkProcessor.tryEmitNext(messages)

    if (emissionCount++ >= clearThreshold) {
      clearEmittedMessages()
      emissionCount = 0
    }
  }

  private fun clearEmittedMessages() {
    synchronized(emittedMessages) {
      emittedMessages.clear()
    }
  }

  fun asFlux(): Flux<List<T>> = sinkProcessor.asFlux()

}
