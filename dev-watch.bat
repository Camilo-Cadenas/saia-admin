@echo off
REM ═══════════════════════════════════════════════════════════════════════════
REM Script de Desarrollo con Recarga Automática
REM ═══════════════════════════════════════════════════════════════════════════
REM Este script compila y ejecuta la aplicación automáticamente cuando
REM detecta cambios en los archivos .java
REM ═══════════════════════════════════════════════════════════════════════════

title SAIA Admin - Desarrollo con Hot Reload

echo.
echo ═══════════════════════════════════════════════════════════════════════════
echo  SAIA ADMIN - MODO DESARROLLO
echo ═══════════════════════════════════════════════════════════════════════════
echo.
echo  Este modo compila automaticamente cuando guardas cambios en el codigo.
echo.
echo  Para detener: Presiona Ctrl+C
echo.
echo ═══════════════════════════════════════════════════════════════════════════
echo.

REM Verificar que Maven esté disponible
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven no esta instalado o no esta en el PATH
    echo.
    echo Instala Maven desde: https://maven.apache.org/download.cgi
    echo O usa mvnw.cmd en lugar de mvn
    pause
    exit /b 1
)

REM Compilación inicial
echo [INFO] Compilacion inicial...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo la compilacion inicial
    pause
    exit /b 1
)

echo.
echo [INFO] Iniciando modo de desarrollo con recarga automatica...
echo [INFO] Guardando cambios activara la recompilacion automatica.
echo.

REM Ejecutar Maven en modo watch
call mvn compile exec:java -Dexec.mainClass="com.saia.Main"

pause
