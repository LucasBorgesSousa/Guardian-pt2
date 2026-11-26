-- Execute com uma conta autorizada a criar banco e tabela. Não apaga dados existentes.
CREATE DATABASE IF NOT EXISTS safetag CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE safetag;
CREATE TABLE IF NOT EXISTS dispositivos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(80) NOT NULL,
    modelo VARCHAR(80) NOT NULL,
    codigo_tag VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dispositivo_tag UNIQUE (codigo_tag)
) ENGINE=InnoDB;
