# PowerShell Script para ejecutar MediClinic Pro
$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Iniciando MediClinic Pro - Sistema Medico" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$binDir = Join-Path $scriptDir "bin"
$libDir = Join-Path $scriptDir "lib"
$launcherClass = Join-Path $binDir "com\mediclinic\Launcher.class"

# 1. Detectar ejecutable java de JDK 21
$javaExe = "java"
if (Test-Path "C:\Program Files\Java\jdk-21\bin\java.exe") {
    $javaExe = "C:\Program Files\Java\jdk-21\bin\java.exe"
} elseif ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaExe = "$env:JAVA_HOME\bin\java.exe"
} else {
    $found = Get-ChildItem "C:\Program Files\Java\jdk-21*\bin\java.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { $javaExe = $found.FullName }
}

# 2. Verificar compilacion
if (-not (Test-Path $launcherClass)) {
    Write-Host "[INFO] Proyecto no compilado. Ejecutando build.ps1..." -ForegroundColor Yellow
    $buildScript = Join-Path $scriptDir "build.ps1"
    & $buildScript
}

Write-Host "Ejecutando con: $javaExe" -ForegroundColor Yellow

# 3. Lanzar la aplicacion JavaFX
$runArgs = @(
    "--module-path", $libDir,
    "--add-modules", "javafx.controls,javafx.fxml",
    "-cp", "$binDir;$libDir\*",
    "com.mediclinic.Launcher"
)

& $javaExe @runArgs
