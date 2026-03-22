@echo off
start "" "%~dp0..\runtime\bin\javaw" -Xmx512m -Dapp.rootDir="%~dp0." -jar "%~dp0gui-1.0.jar" %* && exit 0 || exit 1
