-- MySQL dump 10.13  Distrib 8.0.44, for Linux (x86_64)
--
-- Host: localhost    Database: gcm_db
-- ------------------------------------------------------
-- Server version	8.0.44-0ubuntu0.22.04.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cities`
--

DROP TABLE IF EXISTS `cities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `city_name` varchar(255) NOT NULL,
  `number_of_maps` int DEFAULT '0',
  `price_one_time` double DEFAULT NULL,
  `price_sub` double DEFAULT NULL,
  `pending_price_one_time` double DEFAULT '-1',
  `pending_price_sub` double DEFAULT '-1',
  `subscription_price` decimal(10,2) NOT NULL DEFAULT '100.00',
  `one_time_price` decimal(10,2) NOT NULL DEFAULT '150.00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cities`
--

LOCK TABLES `cities` WRITE;
/*!40000 ALTER TABLE `cities` DISABLE KEYS */;
INSERT INTO `cities` VALUES (1,'Haifa',2,25.9,75.9,-1,-1,100.00,150.00),(2,'Ness Ziona',0,23.9,60.9,-1,-1,100.00,150.00),(3,'Tel Aviv',1,35.9,100.9,-1,-1,100.00,150.00),(4,'Carmiel',0,4.9,12.9,-1,-1,100.00,150.00),(5,'Holon',0,13.9,35.9,-1,-1,100.00,150.00);
/*!40000 ALTER TABLE `cities` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maps`
--

DROP TABLE IF EXISTS `maps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maps` (
  `id` int NOT NULL AUTO_INCREMENT,
  `city_id` int NOT NULL,
  `map_name` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `version` varchar(50) DEFAULT 'v1',
  `price` double NOT NULL,
  `status` enum('DRAFT','PENDING_APPROVAL','PUBLISHED','ARCHIVED') DEFAULT 'DRAFT',
  `one_time_price` decimal(10,2) NOT NULL DEFAULT '25.00',
  PRIMARY KEY (`id`),
  KEY `city_id` (`city_id`),
  CONSTRAINT `maps_ibfk_1` FOREIGN KEY (`city_id`) REFERENCES `cities` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maps`
--

LOCK TABLES `maps` WRITE;
/*!40000 ALTER TABLE `maps` DISABLE KEYS */;
INSERT INTO `maps` VALUES (1,1,'Downtown','Basic city map','v1',35,'PUBLISHED',25.00),(2,1,'Downtown','Includes attractions','v2',34.1,'PUBLISHED',25.00),(3,3,'Nightlife','Bars and clubs','v1',65,'PUBLISHED',25.00),(4,2,'Science Park','High-tech zone and restaurants','v1',24.9,'PUBLISHED',25.00),(5,2,'Cultural Hall Area','City center and cultural hall','v1',25,'PUBLISHED',25.00),(6,4,'Big Center','Shopping center and industrial zone','v1',16.9,'PUBLISHED',25.00),(7,5,'Design Museum','Museum complex and surroundings','v1',19.9,'PUBLISHED',25.00),(8,5,'Yamit Park','Water park and recreation area','v1',22,'PUBLISHED',25.00);
/*!40000 ALTER TABLE `maps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pending_price_updates`
--

DROP TABLE IF EXISTS `pending_price_updates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pending_price_updates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `map_id` int NOT NULL,
  `requester_user_id` int DEFAULT NULL,
  `old_price` decimal(10,2) NOT NULL,
  `new_price` decimal(10,2) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('PENDING','APPROVED','DENIED') NOT NULL DEFAULT 'PENDING',
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `reviewer_user_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pending_user` (`requester_user_id`),
  KEY `idx_pending_map_id` (`map_id`),
  KEY `fk_pending_reviewer` (`reviewer_user_id`),
  KEY `idx_pending_status` (`status`),
  CONSTRAINT `fk_pending_map` FOREIGN KEY (`map_id`) REFERENCES `maps` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_pending_reviewer` FOREIGN KEY (`reviewer_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_pending_user` FOREIGN KEY (`requester_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pending_price_updates`
--

LOCK TABLES `pending_price_updates` WRITE;
/*!40000 ALTER TABLE `pending_price_updates` DISABLE KEYS */;
INSERT INTO `pending_price_updates` VALUES (22,1,4,35.00,100.00,'2026-01-08 15:42:05','PENDING',NULL,NULL);
/*!40000 ALTER TABLE `pending_price_updates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchases`
--

DROP TABLE IF EXISTS `purchases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchases` (
  `purchase_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `city_id` int NOT NULL,
  `map_id` int DEFAULT NULL,
  `purchase_type` varchar(20) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `purchase_date` date NOT NULL,
  `snapshot_version` int DEFAULT '1',
  PRIMARY KEY (`purchase_id`),
  KEY `user_id` (`user_id`),
  KEY `map_id` (`map_id`),
  CONSTRAINT `purchases_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `purchases_ibfk_2` FOREIGN KEY (`map_id`) REFERENCES `maps` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchases`
--

LOCK TABLES `purchases` WRITE;
/*!40000 ALTER TABLE `purchases` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `user_role` enum('CLIENT','EMPLOYEE','CONTENT_MANAGER','COMPANY_MANAGER','CONTENT_WORKER','SUPPORT_AGENT') NOT NULL,
  `failed_attempts` int NOT NULL DEFAULT '0',
  `is_blocked` tinyint(1) NOT NULL DEFAULT '0',
  `id_number` varchar(20) DEFAULT NULL,
  `card_last4` varchar(4) DEFAULT NULL,
  `subscription_expiry` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'manager','123456','Dana','Levi',NULL,'COMPANY_MANAGER',0,0,NULL,NULL,NULL),(2,'worker','123456','Roni','Cohen',NULL,'CONTENT_MANAGER',0,0,NULL,NULL,NULL),(3,'karmant8@gmail.com','12345!','Renat','Karimov','karmant8@gmail.com','COMPANY_MANAGER',0,0,'324511369','1234',NULL),(4,'renato30@gmail.com','123456!','Renato','Moicano','renato30@gmail.com','CONTENT_MANAGER',0,0,'324511333','4321',NULL),(6,'eyal@mail.com','123123!','Eyall','Manager','eyal@mail.com','COMPANY_MANAGER',0,0,NULL,NULL,NULL),(7,'mail@mail.com','123456','User','Buyer','mail@mail.com','CLIENT',0,0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-01-13 16:40:07
