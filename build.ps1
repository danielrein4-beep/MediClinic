# PowerShell Script para compilar MediClinic Pro
$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Compilando MediClinic Pro (Java 21 + JavaFX)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$binDir = Join-Path $scriptDir "bin"
$libDir = Join-Path $scriptDir "lib"
$srcDir = Join-Path $scriptDir "src\main\java"
$resourcesDir = Join-Path $scriptDir "src\main\resources"

# 1. Detectar javac de JDK 21
$javacExe = "javac"
if (Test-Path "C:\Program Files\Java\jdk-21\bin\javac.exe") {
    $javacExe = "C:\Program Files\Java\jdk-21\bin\javac.exe"
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
    $javacExe = "$env:JAVA_HOME\bin\javac.exe"
} else {
    $found = Get-ChildItem "C:\Program Files\Java\jdk-21*\bin\javac.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $javacExe = $found.FullName }
}

Write-Host "Usando compilador: $javacExe" -ForegroundColor Yellow

# 2. Crear carpeta bin
if (-not (Test-Path $binDir)) {
    New-Item -ItemType Directory -Path $binDir | Out-Null
}

# 3. Obtener archivos java
$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
if (-not $javaFiles) {
    Write-Error "No se encontraron archivos Java en $srcDir"
    exit 1
}

# 4. Compilar
Write-Host "Compilando clases Java con JavaFX..." -ForegroundColor Yellow
$compileArgs = @(
    "-encoding", "UTF-8",
    "-d", $binDir,
    "--module-path", $libDir,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-cp", "$libDir\*"
) + $javaFiles

& $javacExe @compileArgs
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error durante la compilacion de clases."
    exit $LASTEXITCODE
}

# 5. Copiar recursos
Write-Host "Copiando recursos (FXML, CSS) a bin..." -ForegroundColor Yellow
if (Test-Path $resourcesDir) {
    Copy-Item -Path "$resourcesDir\*" -Destination $binDir -Recurse -Force
}

Write-Host "===================================================" -ForegroundColor Green
Write-Host "  Compilacion exitosa." -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
