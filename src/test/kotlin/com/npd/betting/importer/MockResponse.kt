package com.npd.betting.importer

val mockResponse = """
        [
  {
    "id": "a20d969267d7244666dee495af9978d5",
    "sport_key": "cricket_ipl",
    "sport_title": "IPL",
    "commence_time": "2023-04-30T14:00:00Z",
    "home_team": "Mumbai Indians",
    "away_team": "Rajasthan Royals",
    "bookmakers": [
      {
        "key": "unibet_eu",
        "title": "Unibet",
        "last_update": "2023-04-30T16:37:34Z",
        "markets": [
          {
            "key": "h2h",
            "last_update": "2023-04-30T16:37:34Z",
            "outcomes": [
              {
                "name": "Mumbai Indians",
                "price": 2.6
              },
              {
                "name": "Rajasthan Royals",
                "price": 1.5
              }
            ]
          }
        ]
      },
      {
        "key": "betfair",
        "title": "Betfair",
        "last_update": "2023-04-30T16:38:03Z",
        "markets": [
          {
            "key": "h2h",
            "last_update": "2023-04-30T16:38:03Z",
            "outcomes": [
              {
                "name": "Mumbai Indians",
                "price": 2.52
              },
              {
                "name": "Rajasthan Royals",
                "price": 1.64
              }
            ]
          },
          {
            "key": "h2h_lay",
            "last_update": "2023-04-30T16:38:03Z",
            "outcomes": [
              {
                "name": "Mumbai Indians",
                "price": 2.56
              },
              {
                "name": "Rajasthan Royals",
                "price": 1.65
              }
            ]
          }
        ]
      }
    ]
  }
]
        """.trimIndent()
