@echo off
setlocal
set MAVEN_VERSION=3.9.11
set MAVEN_HOME=%~dp0.mvn\apache-maven-%MAVEN_VERSION%
if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run
set ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip
set URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
echo Maven nao encontrado. Baixando Apache Maven %MAVEN_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%URL%' -OutFile '%ZIP%'"
if errorlevel 1 exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%~dp0.mvn'"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" exit /b 1
:run
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
