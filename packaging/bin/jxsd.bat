@echo off
rem Launcher for the jxsd CLI. Resolves the bundled jar relative to this script,
rem honoring JXSD_OPTS for extra JVM options (e.g. -Xmx512m).
set "JAR=%~dp0..\lib\jxsd.jar"
if not exist "%JAR%" goto missing
java %JXSD_OPTS% -jar "%JAR%" %*
exit /b %ERRORLEVEL%

:missing
echo jxsd: cannot find "%JAR%" 1>&2
exit /b 1
