@echo off
start "" "%~dp0..\runtime\bin\javaw" -Xmx512m -Dsun.java2d.uiScale=1.0 -Dapp.rootDir="%~dp0." -jar "%~dp0gui.jar" %* && exit 0 || exit 1
