@echo off
setlocal enabledelayedexpansion
set GRADLE=E:\gradle-dist\gradle-8.14\bin\gradle.bat
set PROJ=%~dp0
rmdir /S /Q "%PROJ%build"
if not exist "%PROJ%release" mkdir "%PROJ%release"
for %%V in (1.20.1 1.20.2 1.20.4 1.20.6 1.21.1 1.21.3 1.21.4 1.21.5 1.21.8 1.21.10 1.21.11) do (
  echo ===== building %%V =====
  set ORG_GRADLE_PROJECT_target=%%V
  pushd "%PROJ%"
  call "%GRADLE%" build --no-daemon --console=plain > "E:\gradle-dist\bb_%%V.log" 2>&1
  popd
  echo ===== %%V exit !ERRORLEVEL! =====
  if exist "%PROJ%build\libs\baritonegui-%%V-1.0.0.jar" copy /Y "%PROJ%build\libs\baritonegui-%%V-1.0.0.jar" "%PROJ%release\" >nul
)
echo ALL_DONE
