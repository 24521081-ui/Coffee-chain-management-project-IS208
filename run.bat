@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"
color 0A
title Phung Loc Coffee - Auto Setup ^& Run Script
echo ===================================================
echo     KHOI DONG HE THONG PHUNG LOC COFFEE
echo ===================================================
echo.

:: 1. KIỂM TRA VÀ CÀI ĐẶT JAVA NẾU CHƯA CÓ
echo [1/3] Kiem tra moi truong Java...
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    color 0E
    echo [!] May ban chua cai Java. Dang tien hanh cai dat Java 21...
    winget install Microsoft.OpenJDK.21 --silent --accept-package-agreements --accept-source-agreements
    echo [OK] Da cai dat Java xong. 
    echo [!] Vui long tat cua so nay va mo lai file run.bat de he thong nhan dien Java!
    pause
    exit
) ELSE (
    echo [OK] Da tim thay Java.
)

:: 2. TỰ ĐỘNG THIẾT LẬP BIẾN MÔI TRƯỜNG JAVA_HOME (Sửa lỗi Maven)
echo.
echo [2/3] Kiem tra bien moi truong JAVA_HOME...
IF "%JAVA_HOME%"=="" (
    :: Dùng lệnh ngầm để ép Java tự khai báo nơi nó đang được cài đặt
    FOR /F "tokens=2* delims==" %%I IN ('java -XshowSettings:properties -version 2^>^&1 ^| find "java.home"') DO (
        :: Cắt bỏ khoảng trắng thừa ở đầu chuỗi
        FOR /F "tokens=*" %%J IN ("%%I") DO set "JAVA_HOME=%%J"
    )
    echo [OK] Da tu dong thiet lap JAVA_HOME = !JAVA_HOME!
) ELSE (
    echo [OK] JAVA_HOME da co san = %JAVA_HOME%
)

:: 3. CHẠY ỨNG DỤNG BẰNG MAVEN WRAPPER
echo.
echo [3/3] Dang Build va Khoi dong ung dung...
echo Vui long cho... (Neu chua co Maven, he thong se tu dong tai ngam)
echo ---------------------------------------------------
echo.

call mvnw.cmd clean javafx:run

echo.
echo ---------------------------------------------------
echo [!] Ung dung da dong hoac xay ra loi.
pause
