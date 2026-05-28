@echo off
echo ==============================================
echo   VANGUARD-VPN UNIFIED BUILD AND PACKAGING SCRIPT  
echo ==============================================

echo [1/3] Compiling all source components...
javac Launcher.java KeyGenerator.java functionality/*.java vpn_proxy/*.java VpnSystemTest.java
if %errorlevel% neq 0 (
    echo CRITICAL ERROR: Compilation failed.
    pause
    exit /b %errorlevel%
)

echo [2/3] Packaging everything into a single unified Vanguard-VPN.jar...
jar --create --file=Vanguard-VPN.jar --main-class=Launcher *.class functionality/*.class vpn_proxy/*.class
if %errorlevel% neq 0 (
    echo CRITICAL ERROR: JAR packaging failed.
    pause
    exit /b %errorlevel%
)

echo [3/3] Cleaning up temporary class files to keep directory clean...
del *.class
del functionality\*.class
del vpn_proxy\*.class

echo ==============================================
echo   SUCCESS: Vanguard-VPN.jar built successfully!
echo ==============================================
echo.
echo Launch options:
echo   - Run Client (auto-elevates):  java -jar Vanguard-VPN.jar client
echo   - Run Server:                 java -jar Vanguard-VPN.jar server
echo   - Run Proxy:                  java -jar Vanguard-VPN.jar proxy
echo   - Run Test Suite:             java -jar Vanguard-VPN.jar test
echo.
pause
