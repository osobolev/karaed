@echo off
start "" "%~dp0..\runtime\bin\javaw" @"%~dp0options.args" -Dapp.rootDir="%~dp0." -jar "%~dp0gui.jar" %* && exit 0 || exit 1
