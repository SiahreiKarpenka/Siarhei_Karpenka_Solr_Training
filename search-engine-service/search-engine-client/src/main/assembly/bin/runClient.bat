@echo off

rem echo.
rem echo ------------------------------------------------
rem echo  Simple identity client 0.0.1-SNAPSHOT
rem echo  Usage: runClient.bat 
rem echo ------------------------------------------------
rem echo.

rem ----------------------------------------
rem compiling classpath...
rem ----------------------------------------
setlocal enableextensions
setlocal enabledelayedexpansion

pushd .
set classpath=lib\classworlds-1.1.jar
for %%i in (*.jar) do (
    set classpath=!classpath:#= !;lib\%%i
)
popd

rem ----------------------------------------
rem executing java...
rem ----------------------------------------

pushd .
cd /d %~dp0%
rem --------------- enable next line for debug ---------------
rem set debug=-Xdebug -Xrunjdwp:transport=dt_socket,address=8181,server=y,suspend=n
"%JAVA_HOME%/bin/java" -cp %classpath% %debug% -Xms128m -Xmx512m -ea -Dclassworlds.conf=conf/content.conf org.codehaus.classworlds.Launcher %*
popd
