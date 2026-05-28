@echo off
chcp 65001 > nul

cd /d "%~dp0"

echo ================================
echo Current folder:
echo %cd%
echo ================================

call "D:\Download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" clean compile -e

if errorlevel 1 (
    echo.
    echo Compile failed. Vui long xem loi phia tren.
    pause
    exit /b 1
)

if not exist "target\classes\com\phungloccoffee\MainApp.class" (
    echo.
    echo Khong tim thay file:
    echo target\classes\com\phungloccoffee\MainApp.class
    echo.
    echo Nghia la MainApp.java chua duoc bien dich thanh MainApp.class.
    pause
    exit /b 1
)

echo.
echo Tim thay MainApp.class. Dang chay JavaFX...
echo.

call "D:\Download\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" javafx:run -e

pause