@echo off
@REM ============================================================
@REM  SAIA - Maven Wrapper para Windows
@REM  Uso: mvnw.cmd [goals]
@REM  Ejemplos:
@REM    mvnw.cmd exec:java          <- ejecutar la aplicacion
@REM    mvnw.cmd clean package      <- compilar y crear fat-jar
@REM    mvnw.cmd clean compile      <- solo compilar
@REM ============================================================

SET "BASE_DIR=%~dp0"
SET "WRAPPER_JAR=%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
SET "MAVEN_PROPS_FILE=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

@REM ── Detectar JAVA_HOME automáticamente ─────────────────────
IF NOT "%JAVA_HOME%"=="" GOTO :java_found

@REM Probar JDK 25
IF EXIST "C:\Program Files\Java\jdk-25.0.3\bin\java.exe" (
    SET "JAVA_HOME=C:\Program Files\Java\jdk-25.0.3"
    GOTO :java_found
)
@REM Probar JDK 24
IF EXIST "C:\Program Files\Java\jdk-24\bin\java.exe" (
    SET "JAVA_HOME=C:\Program Files\Java\jdk-24"
    GOTO :java_found
)
@REM Probar ruta del sistema
WHERE java >nul 2>&1
IF %ERRORLEVEL%==0 (
    FOR /f "tokens=*" %%i IN ('where java') DO (
        SET "JAVA_EXE=%%i"
        GOTO :java_from_where
    )
)
echo [ERROR] No se encontro Java. Instale JDK 17 o superior.
EXIT /b 1

:java_from_where
SET "JAVA_EXE=%JAVA_EXE%"
GOTO :run

:java_found
SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

:run
echo [SAIA] Usando Java: %JAVA_EXE%
echo [SAIA] Directorio: %BASE_DIR%

"%JAVA_EXE%" ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

EXIT /b %ERRORLEVEL%
