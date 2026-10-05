
@echo off
setlocal EnableExtensions EnableDelayedExpansion

title Product RDS - Automatic SSM Tunnel

REM ============================================================
REM CONFIGURATION
REM ============================================================

set "PROFILE=terraform-dev"
set "REGION=us-east-1"

set "BASTION_NAME=inventory-project-dev-bastion"

set "RDS=inventory-project-dev-inventory-postgres.c43w4qi8052a.us-east-1.rds.amazonaws.com"

set "LOCAL_PORT=5434"
set "REMOTE_PORT=5432"


REM ============================================================
REM HEADER
REM ============================================================

cls

echo.
echo ============================================================
echo       PRODUCT RDS - AUTOMATIC SSM PORT FORWARD
echo ============================================================
echo.
echo Bastion Name : %BASTION_NAME%
echo RDS          : %RDS%
echo Local Port   : %LOCAL_PORT%
echo Remote Port  : %REMOTE_PORT%
echo.
echo ============================================================
echo.


REM ============================================================
REM CHECK AWS CLI
REM ============================================================

echo [1/5] Checking AWS CLI...

where aws >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: AWS CLI not found.
    goto :ERROR
)

echo       OK


REM ============================================================
REM CHECK SESSION MANAGER PLUGIN
REM ============================================================

echo.
echo [2/5] Checking Session Manager Plugin...

where session-manager-plugin >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: Session Manager Plugin not found.
    goto :ERROR
)

echo       OK


REM ============================================================
REM CHECK AWS CREDENTIALS
REM ============================================================

echo.
echo [3/5] Checking AWS credentials...

aws sts get-caller-identity ^
    --profile "%PROFILE%" ^
    --region "%REGION%" >nul 2>&1

if errorlevel 1 (
    echo.
    echo ERROR: AWS profile "%PROFILE%" is not valid.
    goto :ERROR
)

echo       OK


REM ============================================================
REM DISCOVER BASTION AUTOMATICALLY
REM ============================================================

echo.
echo [4/5] Finding Bastion automatically...

set "BASTION="

for /f "delims=" %%A in ('aws ec2 describe-instances ^
    --filters "Name=tag:Name,Values=%BASTION_NAME%" "Name=instance-state-name,Values=running" ^
    --profile "%PROFILE%" ^
    --region "%REGION%" ^
    --query "Reservations[].Instances[].InstanceId" ^
    --output text 2^>nul') do (

    set "BASTION=%%A"
)

if not defined BASTION (
    echo.
    echo ERROR: No running bastion found.
    echo.
    echo Expected Name tag:
    echo %BASTION_NAME%
    goto :ERROR
)

if "%BASTION%"=="None" (
    echo.
    echo ERROR: Bastion was not found.
    goto :ERROR
)

echo       Bastion found:
echo       %BASTION%


REM ============================================================
REM CHECK SSM
REM ============================================================

echo.
echo       Checking SSM connection...

set "SSM_STATUS="

for /f "delims=" %%A in ('aws ssm describe-instance-information ^
    --filters "Key=InstanceIds,Values=%BASTION%" ^
    --profile "%PROFILE%" ^
    --region "%REGION%" ^
    --query "InstanceInformationList[0].PingStatus" ^
    --output text 2^>nul') do (

    set "SSM_STATUS=%%A"
)

if not defined SSM_STATUS (
    set "SSM_STATUS=None"
)

echo       SSM Status: !SSM_STATUS!

if /I "!SSM_STATUS!" NEQ "Online" (
    echo.
    echo ERROR: Bastion is not Online in SSM.
    echo.
    echo Instance: %BASTION%
    echo Status:   !SSM_STATUS!
    goto :ERROR
)

echo       OK - SSM Online


REM ============================================================
REM CHECK LOCAL PORT
REM ============================================================

echo.
echo       Checking localhost:%LOCAL_PORT%...

netstat -ano | findstr ":%LOCAL_PORT%" >nul 2>&1

if not errorlevel 1 (
    echo.
    echo ERROR: Port %LOCAL_PORT% is already in use.
    echo.
    netstat -ano | findstr ":%LOCAL_PORT%"
    goto :ERROR
)

echo       OK - Port available


REM ============================================================
REM START TUNNEL
REM ============================================================

echo.
echo ============================================================
echo                  STARTING TUNNEL
echo ============================================================
echo.
echo Bastion:
echo   %BASTION%
echo.
echo RDS:
echo   %RDS%
echo.
echo DBeaver:
echo   Host: localhost
echo   Port: %LOCAL_PORT%
echo   Database: inventory
echo.
echo ============================================================
echo.
echo Keep this window OPEN while using DBeaver.
echo.

aws ssm start-session ^
    --target "%BASTION%" ^
    --document-name "AWS-StartPortForwardingSessionToRemoteHost" ^
    --parameters "host=%RDS%,portNumber=%REMOTE_PORT%,localPortNumber=%LOCAL_PORT%" ^
    --profile "%PROFILE%" ^
    --region "%REGION%"

set "EXIT_CODE=%ERRORLEVEL%"

echo.

if not "%EXIT_CODE%"=="0" (
    echo ============================================================
    echo                    TUNNEL FAILED
    echo ============================================================
    echo.
    echo AWS Exit Code: %EXIT_CODE%
    echo Bastion: %BASTION%
    echo.
    goto :ERROR
)

echo Tunnel closed.


:END
echo.
pause
endlocal
exit /b 0


:ERROR
echo.
echo ============================================================
echo                  CONNECTION FAILED
echo ============================================================
echo.
echo Profile : %PROFILE%
echo Region  : %REGION%
echo Bastion : %BASTION%
echo RDS     : %RDS%
echo.
echo DBeaver tunnel was NOT established.
echo.
pause
endlocal
exit /b 1

