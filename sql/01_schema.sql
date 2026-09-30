-- MiniLPP - schéma PostgreSQL de la base de gestion de prévoyance professionnelle
-- Règle : tout montant en DECIMAL, jamais en FLOAT/DOUBLE.

CREATE TABLE IF NOT EXISTS parametre_lpp (
    annee                   SMALLINT       NOT NULL PRIMARY KEY,
    seuil_entree            DECIMAL(12,2)  NOT NULL,
    deduction_coordination  DECIMAL(12,2)  NOT NULL,
    salaire_max_lpp         DECIMAL(12,2)  NOT NULL,
    salaire_coordonne_min   DECIMAL(12,2)  NOT NULL,
    taux_interet_minimal    DECIMAL(6,4)   NOT NULL,
    taux_conversion         DECIMAL(6,4)   NOT NULL,
    source                  VARCHAR(120)
);
COMMENT ON COLUMN parametre_lpp.taux_interet_minimal IS 'ex. 0.0125 = 1.25%';
COMMENT ON COLUMN parametre_lpp.taux_conversion IS 'ex. 0.0680 = 6.8%';
COMMENT ON COLUMN parametre_lpp.source IS 'origine de la valeur, à vérifier chaque année';

CREATE TABLE IF NOT EXISTS bareme_bonification (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    age_min   SMALLINT      NOT NULL,
    age_max   SMALLINT      NOT NULL,
    taux      DECIMAL(6,4)  NOT NULL,
    CONSTRAINT uk_bareme_tranche UNIQUE (age_min, age_max)
);
COMMENT ON COLUMN bareme_bonification.taux IS 'ex. 0.1000 = 10%';

CREATE TABLE IF NOT EXISTS assure (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    no_avs                CHAR(16)      NOT NULL UNIQUE,
    nom                   VARCHAR(80)   NOT NULL,
    prenom                VARCHAR(80)   NOT NULL,
    date_naissance        DATE          NOT NULL,
    sexe                  CHAR(1)       NOT NULL DEFAULT 'X' CHECK (sexe IN ('F','M','X')),
    taux_activite         SMALLINT      NOT NULL DEFAULT 100,
    salaire_annuel_brut   DECIMAL(12,2) NOT NULL,
    date_entree           DATE          NOT NULL,
    date_sortie           DATE,
    actif                 BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_taux_activite CHECK (taux_activite BETWEEN 1 AND 100)
);
COMMENT ON COLUMN assure.no_avs IS 'format 756.XXXX.XXXX.XX';

CREATE INDEX IF NOT EXISTS idx_assure_nom ON assure (nom, prenom);

CREATE TABLE IF NOT EXISTS avoir_vieillesse (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assure_id      BIGINT        NOT NULL REFERENCES assure(id) ON DELETE CASCADE,
    annee          SMALLINT      NOT NULL,
    avoir_initial  DECIMAL(14,2) NOT NULL,
    bonification   DECIMAL(14,2) NOT NULL,
    interet        DECIMAL(14,2) NOT NULL,
    avoir_final    DECIMAL(14,2) NOT NULL,
    CONSTRAINT uk_avoir_annee UNIQUE (assure_id, annee)
);

CREATE TABLE IF NOT EXISTS prestation (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    assure_id       BIGINT        NOT NULL REFERENCES assure(id) ON DELETE CASCADE,
    type            VARCHAR(20)   NOT NULL CHECK (type IN ('RENTE_VIEILLESSE','RENTE_INVALIDITE','RENTE_CONJOINT','CAPITAL_DECES')),
    date_effet      DATE          NOT NULL,
    montant_annuel  DECIMAL(14,2) NOT NULL
);
