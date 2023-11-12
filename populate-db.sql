-- Insert example wallet for user 1
INSERT INTO wallets (user_id, balance)
VALUES (1, 100.00);

-- Insert example events
INSERT INTO events (is_live, name, start_time, sport)
VALUES (true, 'Premier League - Liverpool vs. Manchester United', '2023-05-01 20:00:00', 'Soccer'),
       (true, 'NBA Finals - Game 1', '2023-06-01 19:30:00', 'Basketball'),
       (false, 'World Series of Poker - Main Event', '2023-07-01 12:00:00', 'Poker');

-- Insert example markets
INSERT INTO markets (is_live, last_updated, name, event_id)
VALUES (true, NOW(), 'Match Odds', 1),
       (true, NOW(), 'Over/Under 2.5 Goals', 1),
       (true, NOW(), 'Point Spread', 2),
       (true, NOW(), 'Moneyline', 2),
       (true, NOW(), 'Total Points', 2),
       (false, NOW(), 'Main Event Winner', 3),
       (false, NOW(), 'First Elimination', 3);

-- Insert example market options
INSERT INTO market_options (last_updated, name, odds, market_id)
VALUES (NOW(), 'Liverpool', 1.85, 1),
       (NOW(), 'Manchester United', 3.25, 1),
       (NOW(), 'Draw', 3.50, 1),
       (NOW(), 'Over 2.5 Goals', 2.10, 2),
       (NOW(), 'Under 2.5 Goals', 1.70, 2),
       (NOW(), 'Los Angeles Lakers -6.5', 1.91, 3),
       (NOW(), 'Boston Celtics +6.5', 1.91, 3),
       (NOW(), 'Los Angeles Lakers', 1.25, 4),
       (NOW(), 'Boston Celtics', 3.75, 4),
       (NOW(), 'Over 200 Points', 1.80, 5),
       (NOW(), 'Under 200 Points', 1.90, 5),
       (NOW(), 'John Doe', 3.50, 6),
       (NOW(), 'Jane Doe', 4.00, 6),
       (NOW(), 'Joe Smith', 6.00, 6),
       (NOW(), 'Mike Johnson', 10.00, 6),
       (NOW(), 'Maria Garcia', 15.00, 6),
       (NOW(), 'Alice Kim', 20.00, 6),
       (NOW(), 'Bob Lee', 25.00, 6),
       (NOW(), 'Chris Evans', 50.00, 6),
       (NOW(), 'David Lee', 100.00, 6);

INSERT INTO bets (user_id, market_option_id, stake, potential_winnings, created_at, status)
VALUES (1, 1, 10.00, 18.50, NOW(), 'PENDING'),
       (1, 5, 5.00, 8.50, NOW(), 'WON'),
       (1, 10, 20.00, 36.00, NOW(), 'PENDING');

select *
from events;

select *
from markets
where event_id = 1;

select *
from market_options
where market_id = 1;

