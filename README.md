# Introduction

This is a sports betting backend implemented in Kotlin and Spring Boot. It features a GraphQL API and a WebSocket subscription API.

This is a work in progress. It is not yet ready for production use yet.

This apps fetches live and pre-match odds from [bets-api.com](https://the-odds-api.com/?ref=ultrabet). 
Register with them using my referral link to support this project.

## Demo

Demo betting frontend: https://www.parabolicbet.com/

The frontend codebase is in my [ultrabet-ui repository](https://github.com/anssip/ultrabet-ui)

## Deployment

This repository contains two applications. The first one is a public GraphQL API that provides odds and other public data.
It also provides a WebSocket subscription API for live score updates. Finally, it is responsible for fetching the event 
fixtures, live scores, and odds from the odds-api.com. The second one is a private GraphQL API that provides betting 
functionality.

The two applications are both deployed to Fly.io

To deploy the application that polls odds-api and provides the public API, run:

```bash
flyctl deploy -c application/fly.toml
```

To deploy the application that provides the private betting API, run:

```bash
flyctl deploy -c betting-api/fly.toml
```

## Roadmap

- Core betting features (ongoing, see TODO below)
- Bet types
  - [x] Single ( done)
  - [x] Parlay (done)
  - [ ] System
- Game field live visualization
- Bet builder Bet365 style
- Back Office
  - Risk management (bettors, alerts, limits, etc)
  - Bet list with voiding
- Wallet integration
- Payment gateway integration
- Multiple languages
- Commercial skinning
- Profit

# TODO

- [ ] Spreads (handicap) market with odds fetching and result setting
- [ ] System bet
- [ ] Event view that shows all markets and options for an event
- [ ] Admin queries and mutations
- [ ] Pagination for bets page
- [ ] Show all score updates on current score mouse hover (?)
- [ ] Proper design and styling for the [betting frontend](https://www.parabolicbet.com/)
- [x] Add another market type: over/under for example
- [x] Set results to bets
- [x] pay out winnings 
- [x] Add a 2nd application (gradle subproject) that contains a secured GraphQL API for placing bets and other actions that require authentication.
- [x] GraphQL subscriptions
- [x] Make it update market_option.last_updated_at
- [x] Entities (for JPA)
- [x] Repositories
- [x] Queries
- [x] Mutations
- [x] Import feeds from bets-api.com
- [x] Bet should have several MarketOptions (long bet)
- [x] Deploy somewhere
- [x] scheduled update of completed events. Canceled events are now left non-completed.
