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
set CLASSPATH=%SWSS_HOME%;%SWSS_HOME%\bin;%SWSS_HOME%\lib\commons-daemon-1.0.15.jar;%SWSS_HOME%\lib\log4j-1.2.16.jar
REM set CLASSPATH=%CLASSPATH%;%SWSS_HOME%\bin

set JAVA_OPTS=

:runDaemon
REM echo Starting SWSS Server
REM %JAVA% %JAVA_OPTS% -cp "%CLASSPATH%" "%SWSS_MAIN%"

REM swss32.exe //DS/swss32

swss32.exe //US//swss32 --DisplayName="Simple Socket Server" --Install="%CD%\swss32.exe" ^
		--Classpath=%CLASSPATH% ^
		--Jvm=auto --StartMode=jvm --StopMode=jvm ^
		--StartClass=com.bluemini.websockets.ServerWrapper --StartMethod=windowsService --StartParams=start ^
		--StopClass=com.bluemini.websockets.ServerWrapper --StopMethod=windowsService --StopParams=stop

REM swss32.exe //TS//swss32 --DisplayName="Simple Socket Server"

ENDLOCAL
@ECHO ON
