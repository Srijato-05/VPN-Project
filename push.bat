@echo off
echo ==============================================
echo   VANGUARD-VPN REPOSITORY PUSH SCRIPT
echo ==============================================

if not exist .git (
    echo [1] Initializing local Git repository...
    git init
    git branch -M main
)

echo [2] Configuring git remote to Vanguard-VPN...
git remote remove origin >nul 2>&1
git remote add origin https://github.com/Srijato-05/Vanguard-VPN

echo [3] Staging all files...
git add Launcher.java build.bat push.bat README.md VpnSystemTest.java KeyGenerator.java config/* functionality/* vpn_proxy/* keys/* data/* openvpn-configs/*

echo [4] Committing codebase...
git commit -m "Initialize Vanguard-VPN codebase with secure launcher, build automation, and tests"

echo [5] Pushing to main...
git push -u origin main
if %errorlevel% neq 0 (
    echo.
    echo Push failed. Fetching and rebasing from remote main...
    git pull origin main --rebase --allow-unrelated-histories
    git push origin main
)

echo Done!
pause
