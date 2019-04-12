@echo off

set "inititalDirectory=%~dp0"

::Build process
echo Build started....
echo.

mvn package

echo Finish!
echo.
