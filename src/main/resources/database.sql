/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `crosspermissions_entities` (
  `entity_id` int(11) NOT NULL AUTO_INCREMENT,
  `entity_uuid` varchar(60) DEFAULT NULL,
  `entity_type` set('group','user') DEFAULT NULL,
  `entity_ladder` int(11) DEFAULT NULL,
  `entity_name` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`entity_id`),
  UNIQUE KEY `crosspermissions_entities_user_uuid_uindex` (`entity_uuid`),
  UNIQUE KEY `crosspermissions_entities_entity_name_uindex` (`entity_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `crosspermissions_options` (
  `entity_id` int(11) DEFAULT NULL,
  `option_name` varchar(250) DEFAULT NULL,
  `option_value` varchar(250) DEFAULT NULL,
  UNIQUE KEY `crosspermissions_options_entity_id_option_name_uindex` (`entity_id`,`option_name`),
  CONSTRAINT `crosspermissions_options_crosspermissions_entities_entity_id_fk` FOREIGN KEY (`entity_id`) REFERENCES `crosspermissions_entities` (`entity_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `crosspermissions_parents` (
  `entity_id` int(11) DEFAULT NULL,
  `parent_id` int(11) DEFAULT NULL,
  UNIQUE KEY `crosspermissions_parents_entity_id_parent_id_uindex` (`entity_id`,`parent_id`),
  CONSTRAINT `crosspermissions_parents_crosspermissions_entities_entity_id_fk` FOREIGN KEY (`entity_id`) REFERENCES `crosspermissions_entities` (`entity_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE IF NOT EXISTS `crosspermissions_permissions` (
  `entity_id` int(11) DEFAULT NULL,
  `permission_name` varchar(250) NOT NULL,
  `permission_value` tinyint(1) DEFAULT NULL,
  UNIQUE KEY `crosspermissions_permissions_entity_id_permission_uindex` (`entity_id`,`permission_name`),
  CONSTRAINT `crosspermission_perms_entity_id_fk` FOREIGN KEY (`entity_id`) REFERENCES `crosspermissions_entities` (`entity_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
