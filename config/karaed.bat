@echo off
start "" "%~dp0jre\bin\javaw" -client -Xmx512m -jar "%~dp0gui-1.0.jar" -r "%~dp0." %* && exit 0 || exit 1
