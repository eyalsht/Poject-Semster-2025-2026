CREATE TABLE IF NOT EXISTS daily_city_activity_stats
(
    id INT AUTO_INCREMENT PRIMARY KEY,
    city_id INT NOT NULL,
    stat_date DATE NOT NULL,

    one_time_purchases INT NOT NULL DEFAULT 0,
    subscriptions INT NOT NULL DEFAULT 0,
    subscription_renewals INT NOT NULL DEFAULT 0,
    views INT NOT NULL DEFAULT 0,
    downloads INT NOT NULL DEFAULT 0,

    UNIQUE KEY uq_city_day (city_id, stat_date),

    CONSTRAINT fk_stats_city FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE IF NOT EXISTS map_view_events
(
    id INT AUTO_INCREMENT PRIMARY KEY,
    city_id INT NOT NULL,
    map_id INT NULL,
    user_id INT NULL,
    viewed_at DATETIME NOT NULL,

    INDEX idx_viewed_at (viewed_at),
    INDEX idx_city_day (city_id, viewed_at),
    CONSTRAINT fk_view_city FOREIGN KEY (city_id) REFERENCES cities(id),
    CONSTRAINT fk_view_map FOREIGN KEY (map_id) REFERENCES maps(id),
    CONSTRAINT fk_view_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS map_download_events
(
    id INT AUTO_INCREMENT PRIMARY KEY,
    city_id INT NOT NULL,
    map_id INT NULL,
    user_id INT NULL,
    downloaded_at DATETIME NOT NULL,
    is_subscriber BOOLEAN NOT NULL DEFAULT FALSE,

    INDEX idx_downloaded_at (downloaded_at),
    INDEX idx_city_day (city_id, downloaded_at),
    CONSTRAINT fk_dl_city FOREIGN KEY (city_id) REFERENCES cities(id),
    CONSTRAINT fk_dl_map FOREIGN KEY (map_id) REFERENCES maps(id),
    CONSTRAINT fk_dl_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 4) Mark renewals on purchases (so you can count subscription renewals)
ALTER TABLE purchases ADD COLUMN is_renewal BOOLEAN NOT NULL DEFAULT FALSE;

