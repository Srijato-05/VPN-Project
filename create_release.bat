@echo off
echo ==============================================
echo   VANGUARD-VPN RELEASE PACKAGING SCRIPT
echo ==============================================

echo [1/3] Running clean build to build Vanguard-VPN.jar...
call build.bat

if not exist Vanguard-VPN.jar (
    echo CRITICAL ERROR: Vanguard-VPN.jar was not built successfully.
    pause
    exit /b 1
)

echo.
echo [2/3] Packing JAR and asset folders into release ZIP...
powershell -Command "Compress-Archive -Path 'Vanguard-VPN.jar', 'config', 'keys', 'openvpn-configs', 'README.md' -DestinationPath 'Vanguard-VPN-Latest.zip' -Force"

if %errorlevel% neq 0 (
    echo CRITICAL ERROR: Failed to compress release package.
    pause
    exit /b %errorlevel%
)

echo.
echo ==============================================
echo   SUCCESS: Vanguard-VPN-Latest.zip created!
echo ==============================================
echo.
echo Upload this zip file directly to your GitHub Release. It includes:
echo   - Vanguard-VPN.jar (Unified binary)
echo   - config/ (System parameters)
echo   - keys/ (Encryption keys)
echo   - openvpn-configs/ (Locations profiles)
echo   - README.md (Technical reference guide)
echo.
pause
