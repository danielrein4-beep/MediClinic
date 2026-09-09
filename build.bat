@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   Compilando MediClinic Pro (Java 21 + JavaFX)
echo ===================================================

:: 1. Detectar Java 21 / JDK
set "JAVAC_EXE="

if exist "C:\Program Files\Java\jdk-21\bin\javac.exe" (
    set "JAVAC_EXE=C:\Program Files\Java\jdk-21\bin\javac.exe"
) else if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC_EXE=%JAVA_HOME%\bin\javac.exe"
) else (
    for /d %%D in ("C:\Program Files\Java\jdk-21*") do (
        if exist "%%D\bin\javac.exe" set "JAVAC_EXE=%%D\bin\javac.exe"
    )
    if not defined JAVAC_EXE (
        for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
            if exist "%%D\bin\javac.exe" set "JAVAC_EXE=%%D\bin\javac.exe"
        )
    )
    if not defined JAVAC_EXE (
        set "JAVAC_EXE=javac"
    )
)

echo Usando compilador: "%JAVAC_EXE%"

:: 2. Crear carpeta bin si no existe
if not exist "%~dp0bin" mkdir "%~dp0bin"

:: 3. Obtener lista de archivos Java
set "SOURCES_FILE=%~dp0sources_temp.txt"
dir /s /b "%~dp0src\main\java\*.java" > "%SOURCES_FILE%"

:: 4. Compilar codigo fuente
echo Compilando clases Java con JavaFX...
"%JAVAC_EXE%" -encoding UTF-8 -d "%~dp0bin" --module-path "%~dp0lib" --add-modules javafx.controls,javafx.fxml -cp "%~dp0lib\*" @"%SOURCES_FILE%"
set COMPILE_STATUS=%ERRORLEVEL%
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"

if %COMPILE_STATUS% neq 0 (
    echo [ERROR] Error durante la compilacion de clases.
    exit /b %COMPILE_STATUS%
)

:: 5. Copiar recursos (FXML, CSS, etc.) a bin
echo Copiando recursos a bin...
if exist "%~dp0src\main\resources" (
    xcopy /E /I /Y /Q "%~dp0src\main\resources\*" "%~dp0bin\" >nul
)

echo ===================================================
echo   Compilacion exitosa.
echo ===================================================
