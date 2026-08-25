@echo off
chcp 65001 >nul
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo.
  echo ERRO: nao foi possivel compilar o projeto.
  echo Verifique se o JDK esta instalado com: java -version e javac -version
  pause
  exit /b 1
)
java -cp out Main
pause