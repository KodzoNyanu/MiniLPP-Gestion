# MiniLPP - démarrage sous Windows (PowerShell)
#
#   .\run.ps1            démarre la base, compile, lance les tests, démarre l'API
#   .\run.ps1 -TestsOnly  compile et lance uniquement les tests (aucune base requise)
#   .\run.ps1 -SkipDb     ne touche pas à Docker (base déjà démarrée)

param(
    [switch]$TestsOnly,
    [switch]$SkipDb
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$driverVersion = '3.5.10'
$driverJar = "lib/mariadb-java-client-$driverVersion.jar"
$driverUrl = "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/$driverVersion/mariadb-java-client-$driverVersion.jar"

# 1. Pilote JDBC ------------------------------------------------------------
if (-not (Test-Path $driverJar)) {
    Write-Host "Téléchargement du pilote JDBC MariaDB $driverVersion (~700 Ko) depuis Maven Central..."
    New-Item -ItemType Directory -Force -Path lib | Out-Null
    Invoke-WebRequest -Uri $driverUrl -OutFile $driverJar
}

# 2. Compilation ------------------------------------------------------------
Write-Host "Compilation..."
New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Recurse -Path src, test -Filter *.java | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -d out $sources
if ($LASTEXITCODE -ne 0) { throw "Échec de la compilation" }

# 3. Tests ------------------------------------------------------------------
Write-Host "`nTests :"
& java -Dfile.encoding=UTF-8 -cp out ch.minilpp.Tests
if ($LASTEXITCODE -ne 0) { throw "Des tests ont échoué" }
if ($TestsOnly) { return }

# 4. Base de données --------------------------------------------------------
if (-not $SkipDb) {
    Write-Host "`nDémarrage de MariaDB (Docker)..."
    & docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw "Docker n'a pas pu démarrer la base" }

    Write-Host "Attente de la disponibilité de la base..."
    for ($i = 0; $i -lt 40; $i++) {
        $etat = (& docker inspect --format '{{.State.Health.Status}}' minilpp-db 2>$null)
        if ($etat -eq 'healthy') { break }
        Start-Sleep -Seconds 2
    }
    if ($etat -ne 'healthy') { Write-Warning "La base n'est pas encore 'healthy' - l'API risque d'échouer." }
}

# 5. API --------------------------------------------------------------------
Write-Host "`nDémarrage de l'API sur http://localhost:8080 (Ctrl+C pour arrêter)`n"
& java -Dfile.encoding=UTF-8 -cp "out;$driverJar" ch.minilpp.Api
