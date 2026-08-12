-- Jeu de données de démonstration.
--
-- ATTENTION : les paramètres ci-dessous correspondent à l'exercice 2025.
-- Ils changent chaque année civile. Avant tout usage sérieux (et avant un
-- entretien), vérifie les valeurs en vigueur auprès de l'OFAS
-- (www.bsv.admin.ch) et corrige la table parametre_lpp. Le fait que ces
-- valeurs soient EN BASE et non dans le code est volontaire : c'est la
-- contrainte réelle d'un logiciel de caisse de pension.

INSERT INTO parametre_lpp
    (annee, seuil_entree, deduction_coordination, salaire_max_lpp,
     salaire_coordonne_min, taux_interet_minimal, taux_conversion, source)
VALUES
    (2025, 22680.00, 26460.00, 90720.00, 3780.00, 0.0125, 0.0680, 'valeurs 2025 - A VERIFIER'),
    (2026, 22680.00, 26460.00, 90720.00, 3780.00, 0.0125, 0.0680, 'copie de 2025 - A METTRE A JOUR')
ON DUPLICATE KEY UPDATE annee = VALUES(annee);

INSERT INTO bareme_bonification (age_min, age_max, taux) VALUES
    (25, 34, 0.0700),
    (35, 44, 0.1000),
    (45, 54, 0.1500),
    (55, 70, 0.1800)
ON DUPLICATE KEY UPDATE taux = VALUES(taux);

INSERT INTO assure
    (no_avs, nom, prenom, date_naissance, sexe, taux_activite, salaire_annuel_brut, date_entree)
VALUES
    ('756.1234.5678.97', 'Ruedin',   'Claire',  '1984-03-12', 'F', 80,  84000.00, '2015-04-01'),
    ('756.2345.6789.01', 'Perrin',   'Marc',    '1970-11-02', 'M', 100, 112000.00, '2001-09-01'),
    ('756.3456.7890.12', 'Aebischer','Nina',   '1999-07-25', 'F', 60,  38400.00, '2023-02-01'),
    ('756.4567.8901.23', 'Da Silva', 'Rui',     '1963-01-30', 'M', 100, 96000.00, '1998-06-15'),
    ('756.5678.9012.34', 'Kolly',    'Sophie',  '1991-05-18', 'F', 100, 74000.00, '2019-10-01'),
    ('756.6789.0123.45', 'Zbinden',  'Luca',    '2004-09-09', 'M', 50,  21000.00, '2025-01-06')
ON DUPLICATE KEY UPDATE nom = VALUES(nom);

-- Un historique d'avoir pour deux assurés, afin d'avoir des données à afficher.
INSERT INTO avoir_vieillesse (assure_id, annee, avoir_initial, bonification, interet, avoir_final)
SELECT a.id, 2024, 210000.00, 5754.00, 2625.00, 218379.00 FROM assure a WHERE a.no_avs = '756.1234.5678.97'
ON DUPLICATE KEY UPDATE avoir_final = VALUES(avoir_final);

INSERT INTO avoir_vieillesse (assure_id, annee, avoir_initial, bonification, interet, avoir_final)
SELECT a.id, 2025, 218379.00, 5754.00, 2729.74, 226862.74 FROM assure a WHERE a.no_avs = '756.1234.5678.97'
ON DUPLICATE KEY UPDATE avoir_final = VALUES(avoir_final);

INSERT INTO avoir_vieillesse (assure_id, annee, avoir_initial, bonification, interet, avoir_final)
SELECT a.id, 2025, 412000.00, 9640.00, 5150.00, 426790.00 FROM assure a WHERE a.no_avs = '756.2345.6789.01'
ON DUPLICATE KEY UPDATE avoir_final = VALUES(avoir_final);
