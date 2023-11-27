# Introduction

This is a sports betting backend implemented in Kotlin and Spring Boot. It features a GraphQL API and a WebSocket subscription API.

This is a work in progress. It is not yet ready for production use yet.

## Demo

Demo betting frontend: https://www.parabolicbet.com/

The frontend codebase is in my [ultrabet-ui repository](https://github.com/anssip/ultrabet-ui)

# TODO

- [ ] Set results to bets and pay out winnings 
- [x] Add a 2nd application (gradle subproject) that contains a secured GraphQL API for placing bets and other actions that require authentication.
- [ ] Map internal errors to GraphQL errors that show properly in the client 
- [x] GraphQL subscriptions
- [x] Make it update market_option.last_updated_at
- [x] Entities (for JPA)
- [x] Repositories
- [x] Queries
- [x] Mutations
- [x] Import feeds from bets-api.com
- [ ] Admin queries and mutations
- [x] Bet should have several MarketOptions (long bet)
- [x] Deploy somewhere
- [ ] Pagination TBD when building the UI
- [ ] add completedEvents query
- [x] scheduled update of completed events. Canceled events are now left non-completed.

## Major work items

- Live event visualization
- Bet builder ala Bet365
- Bet types
    - Right
    - Parlay
    - System
- Multifeed
- Back Office
    - Risk management (alerts, limits, etc)
    - Bet list with voiding
    - Player list?
- Wallet integration
- Multiple languages

## Event import

- [x] add a 'source' field to the Market entity, to identify the bookmaker. Event.markets edge should be able to filter
  based on source.
- [x] import each event only once

### Live events

- [ ] Rename to parabolicbet.com
- [x] Live events polling should be more frequent than pre-match events
- [x] Should also request scores when live
- [ ] Check `completed` field when polling scores. When completed -> set result and pay out winnings
- [ ] Show all score updates on current score mouse hover
- [ ] Add results page to show completed events

## Frontend

## Admin

- limits
- alerts
- bet invalidations

## Wallet integration
