# TODO

- [x] Entities (for JPA)
- [x] Repositories
- [x] Queries
- [x] Mutations
- [ ] Import feeds from betapi.com
- [ ] Admin queries and mutations
- [ ] Bet should have several MarketOptions (long bet)
- [ ] Result setting, enhance schemas with results
- [ ] Deploy somewhere

## Event import

- [x] add a 'source' field to the Market entity, to identify the bookmaker. Event.markets edge should be able to filter
  based on source.
- [ ] import each event only once

### Live events

- [ ] Live events polling should be more frequent than pre-match events
- [ ] Should also request scores when live
- [ ] Check `completed` field when polling scores. When completed -> set result and pay out winnings

## Frontend

## Admin

- limits
- alerts
- bet invalidations

## Wallet integration
