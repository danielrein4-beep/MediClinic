@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   Iniciando MediClinic Pro - Sistema Medico
echo ===================================================

:: 1. Detectar ejecutable Java 21
set "JAVA_EXE="

if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe"
) else if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    for /d %%D in ("C:\Program Files\Java\jdk-21*") do (
        if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
    )
    if not defined JAVA_EXE (
        for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
            if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
        )
    )
    if not defined JAVA_EXE (
        set "JAVA_EXE=java"
    )
)

:: 2. Definir rutas del proyecto
set "BIN_DIR=%~dp0bin"
set "LIB_DIR=%~dp0lib"

:: 3. Verificar si el proyecto esta compilado; si no, compilar automaticamente
if not exist "%BIN_DIR%\com\mediclinic\Launcher.class" (
    echo [INFO] Proyecto no compilado aun. Ejecutando compilacion...
    call "%~dp0build.bat"
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] No se pudo compilar el proyecto.
        pause
        exit /b 1
    )
)

:: 4. Ejecutar la aplicacion
echo Ejecutando con Java: "%JAVA_EXE%"
"%JAVA_EXE%" --module-path "%LIB_DIR%" --add-modules javafx.controls,javafx.fxml -cp "%BIN_DIR%;%LIB_DIR%\*" com.mediclinic.Launcher

if %ERRORLEVEL% neq 0 (
    echo.
    echo [AVISO] La aplicacion se ha cerrado con codigo de salida %ERRORLEVEL%.
    pause
)
