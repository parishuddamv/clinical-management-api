# Docker Desktop WSL2 Timeout - Troubleshooting Guide

## Issue
**"Docker Desktop distro installation timeout"** - WSL2 backend cannot be initialized

## Solutions (Try in Order)

### Solution 1: Reset Docker Desktop (Quickest)
1. Open Docker Desktop
2. Click the gear icon (Settings)
3. Navigate to **Troubleshoot**
4. Click **Reset** → **Reset to Factory Defaults**
5. Wait 5 minutes
6. Restart computer
7. Test: `docker ps`

### Solution 2: Switch to Hyper-V Backend (If Pro/Enterprise)
1. Open **Docker Desktop Settings**
2. Go to **General** tab
3. **Uncheck** "Use the WSL 2 based engine"
4. **Check** "Use Hyper-V based engine"
5. Restart Docker Desktop
6. Wait 3 minutes
7. Test: `docker ps`

### Solution 3: Repair WSL2 (Admin PowerShell required)
```powershell
# Run as Administrator
wsl --update
wsl --shutdown
wsl --set-version Docker-Desktop 2
wsl -l -v
```

### Solution 4: Full WSL Reinstall (Nuclear Option - Admin)
```powershell
# Run as Administrator
dism.exe /Online /Disable-Feature /FeatureName:Microsoft-Windows-Subsystem-Linux /All /NoRestart
dism.exe /Online /Enable-Feature /FeatureName:Microsoft-Windows-Subsystem-Linux /All /NoRestart
# Restart computer
wsl --install
```

### Solution 5: Skip Docker - Use Local Java Backend
If Docker continues to fail, we can run Spring Boot services directly:
- Build: `mvn clean install -f pom.xml`
- Run services locally on different ports
- Use local PostgreSQL instead of container

---

## Recommended Immediate Action
1. **Try Solution 1 first** (Reset Docker) - takes 5 minutes
2. If that fails, try **Solution 2** (Switch to Hyper-V)
3. Last resort: Use **Solution 5** (Local Java backend without Docker)

