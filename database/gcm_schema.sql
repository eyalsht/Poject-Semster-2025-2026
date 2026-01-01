CREATE DATABASE IF NOT EXISTS gcm_db;
USE gcm_db;

CREATE TABLE users(
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(255) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            first_name VARCHAR(255),
            last_name VARCHAR(255),
            email VARCHAR(255),
            failed_attempts INT DEFAULT 0,
            is_blocked BOOLEAN DEFAULT 0,
            user_role ENUM('Client', 'Content_Manager', 'Company_Manager', 'Content_Worker','Support_Agent') NOT NULL
);

CREATE TABLE cities (
            id INT AUTO_INCREMENT PRIMARY KEY,
            city_name VARCHAR(255) NOT NULL,
            number_of_maps INT DEFAULT 0,
            price_one_time DOUBLE,
            price_sub DOUBLE,
            pending_price_one_time DOUBLE DEFAULT -1,
            pending_price_sub DOUBLE DEFAULT -1
);

CREATE TABLE maps (
            id INT AUTO_INCREMENT PRIMARY KEY,
            city_id INT NOT NULL,
            map_name VARCHAR(255) NOT NULL,
            description VARCHAR(255),
            version VARCHAR(50) DEFAULT 'v1',
            price DOUBLE NOT NULL,
            status ENUM('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
            FOREIGN KEY (city_id) REFERENCES cities(id)
);

INSERT INTO cities (city_name, number_of_maps, price_sub, price_one_time) VALUES
    ('Haifa', 9, 75.90, 25.90),
    ('Ness Ziona', 5, 60.90, 23.90),
    ('Tel Aviv', 3, 100.90, 35.90),
    ('Carmiel', 12, 12.90, 4.90),
    ('Holon', 2, 35.90, 13.90);

INSERT INTO users (username, password, first_name, last_name, user_role) VALUES
    ('manager', '123456', 'Dana', 'Levi', 'Company_Manager'),
    ('worker', '123456', 'Roni', 'Cohen', 'Content_Manager');

-- sample maps (so your catalog table can show something)
INSERT INTO maps (city_id, map_name, description, version, price, status)
SELECT id, 'Downtown', 'Basic city map', 'v1', 29.90, 'PUBLISHED' FROM cities WHERE city_name='Haifa'
UNION ALL
SELECT id, 'Downtown', 'Includes attractions', 'v2', 39.90, 'PUBLISHED' FROM cities WHERE city_name='Haifa'
UNION ALL
SELECT id, 'Nightlife', 'Bars and clubs', 'v1', 49.90, 'PUBLISHED' FROM cities WHERE city_name='Tel Aviv';
