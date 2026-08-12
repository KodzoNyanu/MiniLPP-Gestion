# MiniLPP — projet fil rouge

Mini-application de gestion de caisse de pension : **Java + MariaDB + HTML/JavaScript**, sans framework.

Elle couvre le cœur métier de la prévoyance professionnelle : détermination du salaire coordonné, bonifications de vieillesse par tranche d'âge, projection de l'avoir jusqu'à la retraite et calcul de la rente au taux de conversion. Les paramètres légaux sont stockés par année civile, jamais codés en dur.

---

## Démarrage en 5 minutes

Prérequis : JDK 17 ou plus, Docker Desktop démarré.

**Windows / PowerShell**

```bash
powershell -ExecutionPolicy Bypass -File run.ps1
```

**Linux / WSL**

```bash
chmod +x run.sh && ./run.sh
```

Le script télécharge le pilote JDBC MariaDB (~700 Ko depuis Maven Central), compile, lance les tests, démarre MariaDB dans Docker sur le port **3307**, puis l'API sur **http://localhost:8080**.

Pour lancer uniquement les tests, sans Docker ni base :

```bash
powershell -ExecutionPolicy Bypass -File run.ps1 -TestsOnly
```

Si le port 8080 est occupé par un autre programme (`Address already in use`), en choisir un autre :

```bash
$env:MINILPP_PORT=8099; powershell -ExecutionPolicy Bypass -File run.ps1 -SkipDb
```

---

## Ce que ça contient

```
minilpp/
├── docker-compose.yml            MariaDB 11 sur le port 3307
├── sql/01_schema.sql             assuré, avoir de vieillesse, paramètres légaux, prestations
├── sql/02_seed.sql               6 assurés de démonstration + paramètres 2025/2026
├── src/ch/minilpp/
│   ├── Db.java                   ouverture de connexion (à remplacer par HikariCP)
│   ├── Api.java                  serveur HTTP du JDK, 3 routes JSON + fichiers statiques
│   ├── Json.java                 sérialisation minimale (à remplacer par Jackson)
│   ├── model/                    Assure, ParametreLpp, BaremeBonification, AnneeProjection
│   ├── dao/                      AssureDao, ParametreLppDao — SQL uniquement, zéro métier
│   └── service/PrevoyanceService.java   salaire coordonné, bonifications, projection, rente
├── test/ch/minilpp/Tests.java    23 assertions, sans dépendance (à migrer vers JUnit 5)
└── web/                          index.html, style.css, app.js (fetch, DOM, SVG à la main)
```

## L'API

| Route | Réponse |
|---|---|
| `GET /api/assures?nom=Ruedin` | liste filtrée, avec salaire coordonné calculé |
| `GET /api/assures/{id}` | fiche : salaire coordonné, bonification, avoir actuel |
| `GET /api/assures/{id}/projection?ageRetraite=65` | projection année par année + rente |

```bash
curl "http://localhost:8080/api/assures/1/projection?ageRetraite=65"
```

## Base de données

```bash
docker exec -it minilpp-db mariadb -u minilpp -pminilpp minilpp
```

Le schéma et les données sont rejoués automatiquement à la **première** création du volume. Pour repartir de zéro :

```bash
docker compose down -v && docker compose up -d
```

---

## Hypothèses de calcul

1. **Les paramètres LPP chargés sont ceux de l'exercice 2025**, marqués « À VÉRIFIER » dans `sql/02_seed.sql`. Ils changent chaque année civile et doivent être confirmés auprès de l'OFAS avant tout usage réel.
2. **Convention retenue pour la projection** : l'intérêt est crédité sur l'avoir au 1er janvier, la bonification est ajoutée en fin d'année. C'est la convention LPP usuelle, mais le règlement d'une caisse peut en prévoir une autre — d'où le choix de garder cette règle isolée dans `PrevoyanceService`.

## Limites connues

Ce n'est pas une application de production. Manquent notamment : un pool de connexions, l'authentification et la gestion des droits, une journalisation structurée, des migrations de schéma versionnées, et le passage des tests maison à JUnit. Ces manques sont assumés et documentés plutôt que masqués ; ils constituent la feuille de route du projet.
