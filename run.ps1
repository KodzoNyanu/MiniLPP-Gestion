# MiniLPP - démarrage sous Windows (PowerShell)
#
#   .\run.ps1             démarre la base, compile, lance les tests, démarre l'API
#   .\run.ps1 -TestsOnly  compile et lance uniquement les tests (aucune base requise)
#   .\run.ps1 -SkipDb     ne touche pas à Docker (base déjà démarrée)
#
# Port de l'API : 8080 par défaut, ou la variable d'environnement MINILPP_PORT.
#
# Ce fichier doit rester enregistré en UTF-8 AVEC BOM : Windows PowerShell 5.1
# lit sinon les accents en ANSI et affiche « TÃ©lÃ©chargement ».

param(
    [switch]$TestsOnly,
    [switch]$SkipDb
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Pour que les accents écrits par Java s'affichent correctement dans la console.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$driverVersion = '42.7.13'
$driverJar = "lib/postgresql-$driverVersion.jar"
$driverUrl = "https://repo1.maven.org/maven2/org/postgresql/postgresql/$driverVersion/postgresql-$driverVersion.jar"
$port = if ($env:MINILPP_PORT) { $env:MINILPP_PORT } else { '8080' }

# Les arguments -D doivent être passés entre guillemets : PowerShell 5.1 découpe
# sinon « -Dfile.encoding=UTF-8 » au point, et java cherche une classe nommée
# « .encoding=UTF-8 ».
$encodage = '-Dfile.encoding=UTF-8'

# 1. Pilote JDBC ------------------------------------------------------------
if (-not (Test-Path $driverJar)) {
    Write-Host "Téléchargement du pilote JDBC PostgreSQL $driverVersion depuis Maven Central..."
    New-Item -ItemType Directory -Force -Path lib | Out-Null
    $ancienneProgression = $ProgressPreference
    $ProgressPreference = 'SilentlyContinue'   # accélère nettement Invoke-WebRequest
    Invoke-WebRequest -Uri $driverUrl -OutFile $driverJar
    $ProgressPreference = $ancienneProgression
}

# 2. Compilation ------------------------------------------------------------
Write-Host "Compilation..."
New-Item -ItemType Directory -Force -Path out | Out-Null
$sources = Get-ChildItem -Recurse -Path src, test -Filter *.java | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -d out $sources
if ($LASTEXITCODE -ne 0) { throw "Échec de la compilation" }

# 3. Tests ------------------------------------------------------------------
Write-Host "`nTests :"
& java $encodage -cp out ch.minilpp.Tests
if ($LASTEXITCODE -ne 0) { throw "Des tests ont échoué" }
if ($TestsOnly) { return }

# 4. Base de données --------------------------------------------------------
if (-not $SkipDb) {
    Write-Host "`nDémarrage de PostgreSQL (Docker)..."
    & docker compose up -d
    if ($LASTEXITCODE -ne 0) { throw "Docker n'a pas pu démarrer la base" }

    Write-Host "Attente de la disponibilité de la base..."
    $etat = ''
    for ($i = 0; $i -lt 40; $i++) {
        $etat = (& docker inspect --format '{{.State.Health.Status}}' minilpp-db 2>$null)
        if ($etat -eq 'healthy') { break }
        Start-Sleep -Seconds 2
    }
    if ($etat -ne 'healthy') {
        Write-Warning "La base n'est pas encore 'healthy' - l'API risque d'échouer."
    }
}

# 5. API --------------------------------------------------------------------
Write-Host "`nDémarrage de l'API sur http://localhost:$port (Ctrl+C pour arrêter)`n"
& java $encodage -cp "out;$driverJar" ch.minilpp.Api
