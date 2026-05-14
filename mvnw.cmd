@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM

@if "%DEBUG%" == "" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%

@REM Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@REM Add default JVM options here. You can also use JAVA_OPTS and MAVEN_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >nul 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@REM Setup the command line

set CLASSPATH=%APP_HOME%\.mvn\wrapper\maven-wrapper.jar

@REM The following line is to avoid `The system cannot find the path specified` error on Windows when MAVEN_HOME contains spaces.
@REM For example: C:\Program Files\Apache Software Foundation\Maven 3.8.1
for %%a in ("%MAVEN_HOME%") do set MAVEN_HOME=%%~sa
for %%a in ("%JAVA_HOME%") do set JAVA_HOME=%%~sa

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
if not "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir
set MAVEN_PROJECTBASEDIR=%CD%
:endDetectBaseDir

@setlocal enableextensions enabledelayedexpansion
if exist "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" (
  for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
)

%JAVA_EXE% %DEFAULT_JVM_OPTS% %JVM_CONFIG_MAVEN_PROPS% %MAVEN_OPTS% %JAVA_OPTS% "-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" "-Dmaven.home=%MAVEN_HOME%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -cp "%CLASSPATH%" org.apache.maven.wrapper.MavenWrapperMain %*
@endlocal

if "%ERRORLEVEL%" == "0" goto end

:fail
set ERROR_CODE=%ERRORLEVEL%

:end
@endlocal & exit /b %ERROR_CODE%
