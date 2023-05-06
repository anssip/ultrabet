CREATE TABLE `users`
(
    `id`       INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(255) NOT NULL UNIQUE,
    `email`    VARCHAR(255) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL
);

CREATE TABLE `wallets`
(
    `id`      INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT            NOT NULL,
    `balance` DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `transactions`
(
    `id`               INT AUTO_INCREMENT PRIMARY KEY,
    `wallet_id`        INT                                                                     NOT NULL,
    `amount`           DECIMAL(10, 2)                                                          NOT NULL,
    `transaction_type` ENUM ('DEPOSIT', 'WITHDRAWAL', 'BET_PLACED', 'BET_WON', 'BET_REFUNDED') NOT NULL,
    `created_at`       TIMESTAMP                                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`) ON DELETE CASCADE
);

CREATE TABLE `events`
(
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `external_id` VARCHAR(255) NULL,
    `is_live`     BOOLEAN      NOT NULL,
    `name`        VARCHAR(255) NOT NULL,
    `start_time`  TIMESTAMP    NOT NULL,
    `sport`       VARCHAR(255) NOT NULL,
    `completed`   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX `unique_events_external_id` ON events (`external_id`);


CREATE TABLE `markets`
(
    `id`           INT AUTO_INCREMENT PRIMARY KEY,
    `source`       VARCHAR(255) NOT NULL DEFAULT 'internal',
    `is_live`      BOOLEAN      NOT NULL,
    `last_updated` TIMESTAMP    NULL,
    `name`         VARCHAR(255) NOT NULL,
    `event_id`     INT          NOT NULL,
    FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `unique_markets_event_id_name_source` ON markets (`event_id`, `name`, `source`);

CREATE TABLE `market_options`
(
    `id`           INT AUTO_INCREMENT PRIMARY KEY,
    `last_updated` TIMESTAMP      NULL,
    `name`         VARCHAR(255)   NOT NULL,
    `odds`         DECIMAL(10, 2) NOT NULL,
    `market_id`    INT            NOT NULL,
    FOREIGN KEY (`market_id`) REFERENCES `markets` (`id`) ON DELETE CASCADE
);

CREATE TABLE `score_updates`
(
    `id`        INT AUTO_INCREMENT PRIMARY KEY,
    `event_id`  INT          NOT NULL,
    `name`      VARCHAR(255) NOT NULL,
    `score`     VARCHAR(255) NOT NULL,
    `timestamp` TIMESTAMP    NOT NULL,
    FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE
);

CREATE TABLE `bets`
(
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    INT                                         NOT NULL,
    `stake`      DECIMAL(10, 2)                              NOT NULL,
    `created_at` TIMESTAMP                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `status`     ENUM ('PENDING', 'WON', 'LOST', 'CANCELED') NOT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
);

CREATE TABLE `bet_options`
(
    `id`               INT AUTO_INCREMENT PRIMARY KEY,
    `bet_id`           INT NOT NULL,
    `market_option_id` INT NOT NULL,
    FOREIGN KEY (`bet_id`) REFERENCES `bets` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`market_option_id`) REFERENCES `market_options` (`id`) ON DELETE CASCADE
);