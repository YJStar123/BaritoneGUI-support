param([string]$targetCsv)
$gradle = "E:/gradle-dist/gradle-8.14/bin/gradle.bat"
$proj = "E:/编程/baritoneGUI支持-Fabric"
$release = "$proj/release"
if (!(Test-Path $release)) { New-Item -ItemType Directory -Path $release | Out-Null }
foreach ($t in ($targetCsv -split ',')) {
    $t = $t.Trim()
    if (!$t) { continue }
    $env:ORG_GRADLE_PROJECT_target = $t
    & $gradle build --no-daemon --console=plain > "$proj/build_$t.log" 2>&1
    $jars = Get-ChildItem "$proj/build/libs" -Filter "*.jar"
    foreach ($j in $jars) { Copy-Item $j.FullName -Destination "$release/$($j.Name)" -Force }
}
Get-ChildItem $release | ForEach-Object { $_.Name }
