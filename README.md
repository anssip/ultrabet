## Demo

Demo betting frontend: https://www.parabolicbet.com/

The frontend codebase is in my [ultrabet-ui repository](https://github.com/anssip/ultrabet-ui)

# TODO

- [x] GraphQL subscriptions
- [x] Make it update market_option.last_updated_at
- [x] Entities (for JPA)
- [x] Repositories
- [x] Queries
- [x] Mutations
- [x] Import feeds from bets-api.com
- [ ] Admin queries and mutations
- [x] Bet should have several MarketOptions (long bet)
- [ ] Result setting, enhance schemas with results
- [x] Deploy somewhere
- [ ] Pagination TBD when building the UI
- [ ] add completedEvents query
- [ ] scheduled update of completed events. Canceled events are now left non-completed.

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
