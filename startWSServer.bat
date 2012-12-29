@ECHO OFF
SETLOCAL

if exist "%JAVA_HOME%\bin\java.exe" goto setJavaHome
set JAVA="java"
goto okJava

:setJavaHome
set JAVA="%JAVA_HOME%\bin\java"

:okJava
if NOT DEFINED SWSS_HOME set SWSS_HOME=%CD%
if NOT DEFINED SWSS_MAIN set SWSS_MAIN=com.bluemini.websockets.SimpleServer

REM Shorten lib path for old platforms
set CLASSPATH=%SWSS_HOME%;%SWSS_HOME%\bin
REM set CLASSPATH=%CLASSPATH%;%SWSS_HOME%\classes

set JAVA_OPTS=

:runDaemon
echo Starting SWSS Server
%JAVA% %JAVA_OPTS% -cp "%CLASSPATH%" "%SWSS_MAIN%"

ENDLOCAL
@ECHO ON
