$ErrorActionPreference = 'Stop'
$c = (Invoke-WebRequest 'https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml' -UseBasicParsing -TimeoutSec 120).Content
($c -split "`n" | Where-Object { $_ -match '<version>1\.(1[0-6])\.\d+</version>' } | ForEach-Object { $_.Trim() }) | Select-Object -Last 40
