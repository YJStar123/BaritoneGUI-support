$ErrorActionPreference = 'Stop'
$base = 'https://maven.fabricmc.net'
$mcs = @('1.20.1','1.20.2','1.20.4','1.20.6','1.21.1','1.21.3','1.21.4','1.21.5','1.21.8','1.21.10','1.21.11','26.1','26.2')

Write-Host "=== YARN ==="
try {
  $yarn = (Invoke-WebRequest "$base/net/fabricmc/yarn/maven-metadata.xml" -UseBasicParsing -TimeoutSec 120).Content
  foreach ($mc in $mcs) {
    $vers = ($yarn -split "`n" | Where-Object { $_ -match "<version>$mc\+build\." } | ForEach-Object { ($_ -replace '.*<version>','') -replace '</version>','' })
    $last = $vers | Select-Object -Last 1
    Write-Host "$mc -> yarn: $last"
  }
} catch { Write-Host "YARN FETCH FAILED: $_" }

Write-Host "=== FABRIC-API ==="
try {
  $api = (Invoke-WebRequest "$base/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml" -UseBasicParsing -TimeoutSec 120).Content
  foreach ($mc in $mcs) {
    $vers = ($api -split "`n" | Where-Object { $_ -match "<version>.*\+$mc</version>" } | ForEach-Object { ($_ -replace '.*<version>','') -replace '</version>','' })
    $last = $vers | Select-Object -Last 1
    Write-Host "$mc -> api: $last"
  }
} catch { Write-Host "API FETCH FAILED: $_" }

Write-Host "=== LOOM ==="
try {
  $loom = (Invoke-WebRequest "$base/net/fabricmc/fabric-loom/maven-metadata.xml" -UseBasicParsing -TimeoutSec 120).Content
  ($loom -split "`n" | Where-Object { $_ -match '<version>1\.(1[0-9]|[0-9])\.' } | ForEach-Object { ($_ -replace '.*<version>','') -replace '</version>','' }) | Select-Object -Last 12 | ForEach-Object { Write-Host "loom: $_" }
} catch { Write-Host "LOOM FETCH FAILED: $_" }
