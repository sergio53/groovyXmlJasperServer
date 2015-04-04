@echo off
:start
@call c:\opt\groovy\bin\groovy.bat -cp %~dp0jrlib.jar JRServer.groovy
goto start