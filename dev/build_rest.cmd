@echo off
setlocal enabledelayedexpansion
set GRADLE=E:\gradle-dist\gradle-8.14\bin\gradle.bat
set PROJ=%~dp0
rmdir /S /Q "%PROJ%build"
if not exist "%PROJ%release" mkdir "%PROJ%release"
pushd "%PROJ%"
for %%V in (1.20.1 1.20.2 1.20.4 1.20.6 1.21.3 1.21.4 1.21.5 1.21.8) do (
  call :buildone %%V
)
popd
echo ALL_DONE
goto :eof

:buildone
set V=%1
set ORG_GRADLE_PROJECT_target=%V%
set TRY=0
:retry
del /Q "C:\Users\14753\.gradle\caches\fabric-loom\.f23fcccd987ec09184541de8eabef2b9fbb91e97.lock" >nul 2>&1
call "%GRADLE%" build --no-daemon --console=plain > "E:\gradle-dist\bb_%V%.log" 2>&1
if exist "%PROJ%build\libs\baritonegui-%V%-1.0.0.jar" (
  copy /Y "%PROJ%build\libs\baritonegui-%V%-1.0.0.jar" "%PROJ%release\" >nul
  echo ===== %V% OK =====
  goto :eof
)
set /a TRY+=1
if !TRY! LSS 2 (
  echo ===== %V% retry !TRY! =====
  goto retry
)
echo ===== %V% FAILED =====
goto :eof
