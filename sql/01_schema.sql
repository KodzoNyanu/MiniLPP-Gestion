-- MiniLPP - schéma de la base de gestion de prévoyance professionnelle
-- Règle : tout montant en DECIMAL, jamais en FLOAT/DOUBLE.

CREATE TABLE IF NOT EXISTS parametre_lpp (
    annee                   SMALLINT       NOT NULL PRIMARY KEY,
    seuil_entree            DECIMAL(12,2)  NOT NULL,
    deduction_coordination  DECIMAL(12,2)  NOT NULL,
    salaire_max_lpp         DECIMAL(12,2)  NOT NULL,
    salaire_coordonne_min   DECIMAL(12,2)  NOT NULL,
    taux_interet_minimal    DECIMAL(6,4)   NOT NULL COMMENT 'ex. 0.0125 = 1.25%',
    taux_conversion         DECIMAL(6,4)   NOT NULL COMMENT 'ex. 0.0680 = 6.8%',
    source                  VARCHAR(120)   NULL COMMENT 'origine de la valeur, à vérifier chaque année'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bareme_bonification (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    age_min   TINYINT       NOT NULL,
    age_max   TINYINT       NOT NULL,
    taux      DECIMAL(6,4)  NOT NULL COMMENT 'ex. 0.1000 = 10%',
    CONSTRAINT uk_bareme_tranche UNIQUE (age_min, age_max)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assure (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    no_avs                CHAR(16)      NOT NULL UNIQUE COMMENT 'format 756.XXXX.XXXX.XX',
    nom                   VARCHAR(80)   NOT NULL,
    prenom                VARCHAR(80)   NOT NULL,
    date_naissance        DATE          NOT NULL,
    sexe                  ENUM('F','M','X') NOT NULL DEFAULT 'X',
    taux_activite         TINYINT       NOT NULL DEFAULT 100,
    salaire_annuel_brut   DECIMAL(12,2) NOT NULL,
    date_entree           DATE          NOT NULL,
    date_sortie           DATE          NULL,
    actif                 BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_taux_activite CHECK (taux_activite BETWEEN 1 AND 100)
) ENGINE=InnoDB;

CREATE INDEX idx_assure_nom ON assure (nom, prenom);

CREATE TABLE IF NOT EXISTS avoir_vieillesse (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    assure_id      BIGINT        NOT NULL,
    annee          SMALLINT      NOT NULL,
    avoir_initial  DECIMAL(14,2) NOT NULL,
    bonification   DECIMAL(14,2) NOT NULL,
    interet        DECIMAL(14,2) NOT NULL,
    avoir_final    DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_avoir_assure FOREIGN KEY (assure_id) REFERENCES assure(id) ON DELETE CASCADE,
    CONSTRAINT uk_avoir_annee UNIQUE (assure_id, annee)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS prestation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    assure_id       BIGINT        NOT NULL,
    type            ENUM('RENTE_VIEILLESSE','RENTE_INVALIDITE','RENTE_CONJOINT','CAPITAL_DECES') NOT NULL,
    date_effet      DATE          NOT NULL,
    montant_annuel  DECIMAL(14,2) NOT NULL,
    CONSTRAINT fk_prestation_assure FOREIGN KEY (assure_id) REFERENCES assure(id) ON DELETE CASCADE
) ENGINE=InnoDB;
