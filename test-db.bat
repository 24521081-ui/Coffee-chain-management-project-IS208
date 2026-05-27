@echo off
chcp 65001
echo ==========================================
echo Testing Oracle Database Connection
echo ==========================================

set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

where mvn >nul 2>nul
if %errorlevel%==0 (
    call mvn clean compile
    if %errorlevel% neq 0 goto end
    call mvn -q dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
    if %errorlevel% neq 0 goto end
    set /p APP_CP=<target\classpath.txt
    call java -cp "target\classes;%APP_CP%" com.phungloccoffee.util.TestDBConnection
) else if exist mvnw.cmd (
    call mvnw.cmd clean compile
    if %errorlevel% neq 0 goto end
    call mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
    if %errorlevel% neq 0 goto end
    set /p APP_CP=<target\classpath.txt
    call java -cp "target\classes;%APP_CP%" com.phungloccoffee.util.TestDBConnection
) else (
    echo Maven is not installed and Maven Wrapper is not available.
)

:end
pause
