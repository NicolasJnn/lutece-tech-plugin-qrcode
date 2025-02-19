
--
-- Structure for table qrcode_config
--

DROP TABLE IF EXISTS qrcode_config;
CREATE TABLE qrcode_config (
id_qr_code_config int AUTO_INCREMENT,
configuration_name varchar(50) default '' NOT NULL,
configuration_type int default '0' NOT NULL,
image_logo varchar(50),
PRIMARY KEY (id_qr_code_config)
);
