@echo off
cd /d "%~dp0"
java -jar "build\SimYukkuri-makaizou.jar"
if errorlevel 1 pause
