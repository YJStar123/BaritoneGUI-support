<#
.SYNOPSIS
    Build BaritoneGUI for a specific Minecraft version.

.DESCRIPTION
    Wraps the Gradle wrapper: it picks the correct Java source tree (src / src-v2),
    mirrors the sources to an ASCII temp directory (so Gradle never has to scan a
    non-ASCII project path), then invokes ./gradlew with the right -Ptarget /
    -PsrcTree / -PmirrorRoot and copies the produced jar into release/.

.PARAMETER ProjRoot
    Project root (defaults to the directory this script lives in).

.PARAMETER Target
    Minecraft version to build, e.g. 1.21.1, 1.21.11. Default: 1.21.1.

.PARAMETER MirrorRoot
    Where to mirror the sources. Defaults to "$env:TEMP\baritonegui-mirror"
    (an ASCII path outside the project). Set to '' to read straight from the
    project tree (only safe when the project path is ASCII).

.PARAMETER Jdk
    Optional JDK home. When set, JAVA_HOME is pointed at it before building.
#>
param(
    [string]$ProjRoot = $PSScriptRoot,
    [string]$Target   = "1.21.1",
    [string]$MirrorRoot = "",
    [string]$Jdk      = ""
)

$enc = New-Object System.Text.UTF8Encoding($false)

if ($ProjRoot -eq "") { $ProjRoot = $PSScriptRoot }
$ProjRoot = Resolve-Path $ProjRoot

# ---- Pick the Java source tree -------------------------------------------
$useV2 = $false
if ($Target.StartsWith('26.')) {
    $useV2 = $true
} elseif ($Target -match '^1\.21\.(\d+)$') {
    if ([int]$Matches[1] -ge 9) { $useV2 = $true }
}
$srcTree = if ($useV2) { 'src-v2' } else { 'src' }
Write-Host "SOURCE_TREE=$srcTree for $Target"

# ---- Mirror sources to an ASCII path (avoids non-ASCII project paths) -----
if ($MirrorRoot -eq "") {
    $MirrorRoot = Join-Path $env:TEMP "baritonegui-mirror"
}
$mirrorJava = Join-Path $MirrorRoot "main\java"
$mirrorRes  = Join-Path $MirrorRoot "main\resources"
if (Test-Path $MirrorRoot) { Remove-Item $MirrorRoot -Recurse -Force }
New-Item -ItemType Directory -Path $mirrorJava -Force | Out-Null
New-Item -ItemType Directory -Path $mirrorRes  -Force | Out-Null
Copy-Item "$ProjRoot\$srcTree\main\java\*"   $mirrorJava -Recurse -Force
Copy-Item "$ProjRoot\src\main\resources\*"   $mirrorRes  -Recurse -Force

# ---- Optional JDK --------------------------------------------------------
if ($Jdk -ne "") {
    $env:JAVA_HOME = $Jdk
    $env:PATH = "$Jdk\bin;" + $env:PATH
}

# ---- Best-effort: clear stale Loom cache locks ---------------------------
Remove-Item "$env:USERPROFILE\.gradle\caches\fabric-loom\*.lock" -Force -ErrorAction SilentlyContinue

# ---- Build ---------------------------------------------------------------
# Pass build properties via ORG_GRADLE_PROJECT_* env vars (Gradle parses
# `-Pkey=value` inconsistently when the value contains dots, e.g. "1.21.1").
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
$env:ORG_GRADLE_PROJECT_target = $Target
$env:ORG_GRADLE_PROJECT_srcTree = $srcTree
$env:ORG_GRADLE_PROJECT_mirrorRoot = $MirrorRoot
$gradlew = Join-Path $ProjRoot "gradlew.bat"
& $gradlew -p $ProjRoot clean build --no-daemon --console=plain --stacktrace *>&1 | Tee-Object -FilePath "$ProjRoot\build_$Target.log"
$ec = $LASTEXITCODE
Write-Host "BUILD_$Target`_EXIT=$ec"

# ---- Publish the artifact on success -------------------------------------
if ($ec -eq 0) {
    $jar = "$ProjRoot\build\libs\baritonegui-$Target-1.0.0.jar"
    if (Test-Path $jar) {
        if (-not (Test-Path "$ProjRoot\release")) { New-Item -ItemType Directory -Path "$ProjRoot\release" -Force | Out-Null }
        Copy-Item $jar "$ProjRoot\release\" -Force
        Write-Host "COPIED baritonegui-$Target-1.0.0.jar -> release/"
    } else {
        Write-Host "WARN: build reported success but jar not found: $jar"
    }
}
